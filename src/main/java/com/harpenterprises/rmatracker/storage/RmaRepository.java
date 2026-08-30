package com.harpenterprises.rmatracker.storage;

import com.harpenterprises.rmatracker.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class  RmaRepository {

    public void save(RmaRecord record) throws SQLException {
        String sql = """
                INSERT INTO rmas (rma_number, date_sent, date_received, status,
                    outgoing_tracking_number, return_tracking_number, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    setRmaValues(ps, record);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not retrieve RMA ID.");
                        id = keys.getLong(1);
                    }
                }
                insertRepairItems(c, id, record.getRepairItems());
                insertShippingInformation(c, id, record.getShippingInformation());
                insertStatusHistory(c, id, null, record.getStatus());
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }


    public void update(RmaRecord record) throws SQLException {

        /*
         * Keep this method for compatibility with
         * existing code that is not renaming an RMA.
         */
        update(
                record.getRmaNumber(),
                record
        );
    }


    public void update(
            String originalRmaNumber,
            RmaRecord record
    ) throws SQLException {

        String sql = """
            UPDATE rmas
            SET rma_number=?,
                date_sent=?,
                date_received=?,
                status=?,
                outgoing_tracking_number=?,
                return_tracking_number=?,
                notes=?,
                updated_at=CURRENT_TIMESTAMP
            WHERE rma_number=?
            """;

        try (Connection c =
                     DatabaseManager.getConnection()) {

            c.setAutoCommit(false);

            try {

                /*
                 * Find the permanent internal database ID
                 * using the OLD RMA number.
                 */
                long id =
                        findRmaId(
                                c,
                                originalRmaNumber
                        );

                Status oldStatus =
                        findCurrentStatus(
                                c,
                                id
                        );

                try (PreparedStatement ps =
                             c.prepareStatement(sql)) {

                    /*
                     * New RMA number.
                     */
                    ps.setString(
                            1,
                            record.getRmaNumber()
                    );

                    setNullableDate(
                            ps,
                            2,
                            record.getDateSent()
                    );

                    setNullableDate(
                            ps,
                            3,
                            record.getDateReceived()
                    );

                    ps.setString(
                            4,
                            record.getStatus().name()
                    );

                    setNullableString(
                            ps,
                            5,
                            record
                                    .getOutgoingTrackingNumber()
                    );

                    setNullableString(
                            ps,
                            6,
                            record
                                    .getReturnTrackingNumber()
                    );

                    setNullableString(
                            ps,
                            7,
                            record.getNotes()
                    );

                    /*
                     * Locate the database record using
                     * the ORIGINAL RMA number.
                     */
                    ps.setString(
                            8,
                            originalRmaNumber
                    );

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "RMA not found: "
                                        + originalRmaNumber
                        );
                    }
                }

                /*
                 * These records are connected using the
                 * permanent database ID, not the visible
                 * RMA number.
                 */
                deleteRepairItems(
                        c,
                        id
                );

                deleteShippingInformation(
                        c,
                        id
                );

                insertRepairItems(
                        c,
                        id,
                        record.getRepairItems()
                );

                insertShippingInformation(
                        c,
                        id,
                        record.getShippingInformation()
                );

                /*
                 * Preserve the existing status-history
                 * behavior.
                 */
                if (!Objects.equals(
                        oldStatus,
                        record.getStatus()
                )) {

                    insertStatusHistory(
                            c,
                            id,
                            oldStatus,
                            record.getStatus()
                    );
                }

                c.commit();

            } catch (SQLException e) {

                c.rollback();
                throw e;

            } finally {

                c.setAutoCommit(true);
            }
        }
    }

    public boolean delete(String rmaNumber) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rmas WHERE rma_number=?")) {
            ps.setString(1, rmaNumber);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<RmaRecord> findByRmaNumber(String rmaNumber) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM rmas WHERE rma_number=?")) {
            ps.setString(1, rmaNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRmaRecord(c, rs)) : Optional.empty();
            }
        }
    }

    public List<RmaRecord> findAll() throws SQLException {
        List<RmaRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM rmas ORDER BY CASE WHEN date_sent IS NULL THEN 1 ELSE 0 END, date_sent DESC, rma_number ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) records.add(mapRmaRecord(c, rs));
        }
        return records;
    }

    public List<RmaRecord> search(String text) throws SQLException {
        if (text == null || text.isBlank()) return findAll();
        String sql = """
                SELECT DISTINCT r.* FROM rmas r
                LEFT JOIN repair_items i ON r.id=i.rma_id
                LEFT JOIN shipping_information s ON r.id=s.rma_id
                WHERE LOWER(r.rma_number) LIKE ? OR LOWER(r.status) LIKE ?
                   OR LOWER(COALESCE(r.notes,'')) LIKE ?
                   OR LOWER(COALESCE(i.county,'')) LIKE ?
                   OR LOWER(COALESCE(i.machine_type,'')) LIKE ?
                   OR LOWER(COALESCE(i.serial_number,'')) LIKE ?
                   OR LOWER(COALESCE(i.version,'')) LIKE ?
                   OR LOWER(COALESCE(i.problem_description,'')) LIKE ?
                   OR LOWER(COALESCE(i.repair_description,'')) LIKE ?
                   OR LOWER(COALESCE(s.carrier,'')) LIKE ?
                   OR LOWER(COALESCE(s.tracking_number,'')) LIKE ?
                ORDER BY r.date_sent DESC, r.rma_number ASC
                """;
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        List<RmaRecord> records = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i=1;i<=11;i++) ps.setString(i, pattern);
            try (ResultSet rs=ps.executeQuery()) { while(rs.next()) records.add(mapRmaRecord(c,rs)); }
        }
        return records;
    }

    public boolean existsByRmaNumber(String number) throws SQLException {
        try (Connection c=DatabaseManager.getConnection(); PreparedStatement ps=c.prepareStatement("SELECT 1 FROM rmas WHERE rma_number=? LIMIT 1")) {
            ps.setString(1,number); try(ResultSet rs=ps.executeQuery()){ return rs.next(); }
        }
    }

    private void insertRepairItems(Connection c,long id,List<RepairItem> items)throws SQLException{
        if(items==null||items.isEmpty())return;
        String sql="INSERT INTO repair_items (rma_id,county,machine_type,serial_number,version,problem_description,repair_description,received) VALUES (?,?,?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(RepairItem i:items){ ps.setLong(1,id); setNullableString(ps,2,i.getCounty()); setNullableString(ps,3,i.getMachineType()); setNullableString(ps,4,i.getSerialNumber()); setNullableString(ps,5,i.getVersion()); setNullableString(ps,6,i.getProblemDescription()); setNullableString(ps,7,i.getRepairDescription()); ps.setInt(8,i.isReceived()?1:0); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    private void insertShippingInformation(Connection c,long id,List<ShippingInfo> shipments)throws SQLException{
        if(shipments==null||shipments.isEmpty())return;
        String sql="INSERT INTO shipping_information (rma_id,carrier,tracking_number,shipping_direction) VALUES (?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(ShippingInfo s:shipments){ ps.setLong(1,id); ps.setString(2,s.getCarrier().trim()); ps.setString(3,s.getTrackingNumber().trim()); ps.setString(4,s.getDirection().name()); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    private List<RepairItem> findRepairItems(Connection c,long id)throws SQLException{
        List<RepairItem> list=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM repair_items WHERE rma_id=? ORDER BY id")){ ps.setLong(1,id); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) list.add(new RepairItem(rs.getString("county"),rs.getString("machine_type"),rs.getString("serial_number"),rs.getString("version"),rs.getString("problem_description"),rs.getString("repair_description"),rs.getInt("received")==1)); }}
        return list;
    }

    private List<ShippingInfo> findShippingInformation(Connection c,long id)throws SQLException{
        List<ShippingInfo> list=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement("SELECT * FROM shipping_information WHERE rma_id=? ORDER BY id")){ ps.setLong(1,id); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) list.add(new ShippingInfo(rs.getString("carrier"),rs.getString("tracking_number"),ShippingDirection.valueOf(rs.getString("shipping_direction")))); }}
        return list;
    }

    private RmaRecord mapRmaRecord(Connection c,ResultSet rs)throws SQLException{
        long id=rs.getLong("id");
        return new RmaRecord(rs.getString("rma_number"),getNullableDate(rs,"date_sent"),getNullableDate(rs,"date_received"),Status.valueOf(rs.getString("status")),rs.getString("outgoing_tracking_number"),rs.getString("return_tracking_number"),rs.getString("notes"),findRepairItems(c,id),findShippingInformation(c,id));
    }

    private void deleteRepairItems(Connection c,long id)throws SQLException{ try(PreparedStatement ps=c.prepareStatement("DELETE FROM repair_items WHERE rma_id=?")){ps.setLong(1,id);ps.executeUpdate();} }
    private void deleteShippingInformation(Connection c,long id)throws SQLException{ try(PreparedStatement ps=c.prepareStatement("DELETE FROM shipping_information WHERE rma_id=?")){ps.setLong(1,id);ps.executeUpdate();} }
    private long findRmaId(Connection c,String n)throws SQLException{ try(PreparedStatement ps=c.prepareStatement("SELECT id FROM rmas WHERE rma_number=?")){ps.setString(1,n);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("RMA not found: "+n);return rs.getLong(1);}} }
    private Status findCurrentStatus(Connection c,long id)throws SQLException{ try(PreparedStatement ps=c.prepareStatement("SELECT status FROM rmas WHERE id=?")){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new SQLException("RMA not found.");return Status.valueOf(rs.getString(1));}} }
    private void insertStatusHistory(Connection c,long id,Status oldS,Status newS)throws SQLException{ try(PreparedStatement ps=c.prepareStatement("INSERT INTO status_history (rma_id,old_status,new_status,changed_at) VALUES (?,?,?,?)")){ps.setLong(1,id);if(oldS==null)ps.setNull(2,Types.VARCHAR);else ps.setString(2,oldS.name());ps.setString(3,newS.name());ps.setString(4,LocalDateTime.now().withNano(0).toString());ps.executeUpdate();} }
    private void setRmaValues(PreparedStatement ps,RmaRecord r)throws SQLException{ps.setString(1,r.getRmaNumber());setNullableDate(ps,2,r.getDateSent());setNullableDate(ps,3,r.getDateReceived());ps.setString(4,r.getStatus().name());setNullableString(ps,5,r.getOutgoingTrackingNumber());setNullableString(ps,6,r.getReturnTrackingNumber());setNullableString(ps,7,r.getNotes());}
    private void setNullableDate(PreparedStatement ps,int i,LocalDate d)throws SQLException{if(d==null)ps.setNull(i,Types.VARCHAR);else ps.setString(i,d.toString());}
    private void setNullableString(PreparedStatement ps,int i,String v)throws SQLException{if(v==null||v.isBlank())ps.setNull(i,Types.VARCHAR);else ps.setString(i,v.trim());}
    private LocalDate getNullableDate(ResultSet rs,String c)throws SQLException{String s=rs.getString(c);return s==null||s.isBlank()?null:LocalDate.parse(s);}
}
