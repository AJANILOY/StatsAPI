package de.snx.simplestats.manager.other;

import de.snx.simplestats.SimpleStats;
import de.snx.simplestats.mysql.DatabaseUpdate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class PlayerStats extends DatabaseUpdate {

    private UUID uuid;
    private int games;
    private int wins;
    private int kills;
    private int deaths;
    private int rank;
    private boolean onlineMode;

    public PlayerStats(UUID uuid) {
        this(uuid, true, true);
    }

    public PlayerStats(UUID uuid, boolean addUpdater, boolean onlineMode) {
        this.uuid = uuid;
        this.kills = 0;
        this.deaths = 0;
        this.rank = 0;
        this.games = 0;
        this.wins = 0;
        this.onlineMode = onlineMode;
        loadDataAsync();
        if (addUpdater) {
            setForceUpdate(true);
            addToUpdater();
        }
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public int getKills() {
        return this.kills;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public int getRank() {
        return this.rank;
    }

    public int getGames() {
        return this.games;
    }

    public int getWins() {
        return this.wins;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public double getKD() {
        if (getKills() <= 0) {
            return 0.0D;
        }
        if (getDeaths() <= 0) {
            return getKills();
        }
        BigDecimal dec = new BigDecimal(getKills() / getDeaths());
        dec = dec.setScale(2, 4);
        return dec.doubleValue();
    }

    public void setGames(int games) {
        this.games = games;
        setUpdate(true);
    }

    public void setWins(int wins) {
        this.wins = wins;
        setUpdate(true);
    }

    public void setKills(int kills) {
        this.kills = kills;
        setUpdate(true);
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
        setUpdate(true);
    }

    public void setRank(int rank) {
        this.rank = rank;
        setUpdate(true);
    }

    public void saveData() {
        try {
            PreparedStatement stCheck = SimpleStats.getSQLManager().getConnection().prepareStatement("SELECT * FROM `SimpleStatsAPI` WHERE `UUID` = ?");
            stCheck.setString(1, getUUID().toString());
            ResultSet rsCheck = SimpleStats.getSQLManager().executeQuery(stCheck);
            if (!rsCheck.next()) {
                PreparedStatement st = SimpleStats.getSQLManager().getConnection().prepareStatement("INSERT INTO `SimpleStatsAPI` (UUID, Games, Wins, Kills, Deaths) VALUES (?, 0, 0, 0, 0)");
                st.setString(1, getUUID().toString());
                SimpleStats.getSQLManager().executeUpdate(st);
            } else {
                PreparedStatement st = SimpleStats.getSQLManager().getConnection().prepareStatement("UPDATE `SimpleStatsAPI` SET `Games` = ?, `Wins` = ?, `Kills` = ?, `Deaths` = ? WHERE `UUID` = ?");
                st.setInt(1, getGames());
                st.setInt(2, getWins());
                st.setInt(3, getKills());
                st.setInt(4, getDeaths());
                st.setString(5, getUUID().toString());
                SimpleStats.getSQLManager().executeUpdate(st);
            }
            rsCheck.close();
            stCheck.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveDataAsync() {
        SimpleStats.getSQLManager().getAsyncHandler().getExecutor().execute(new Runnable() {
            public void run() {
                PlayerStats.this.saveData();
            }
        });
    }

    public void loadData() {
        try {
            PreparedStatement st = SimpleStats.getSQLManager().getConnection().prepareStatement("SELECT * FROM `SimpleStatsAPI` WHERE `UUID` = ?");
            st.setString(1, getUUID().toString());
            ResultSet rs = SimpleStats.getSQLManager().executeQuery(st);
            if (!rs.next()) {
                saveData();
            } else {
                this.kills = rs.getInt("Kills");
                this.deaths = rs.getInt("Deaths");
                this.wins = rs.getInt("Wins");
                this.games = rs.getInt("Games");
            }
            ResultSet rs2 = SimpleStats.getSQLManager().executeQuery("SELECT * FROM `SimpleStatsAPI` ORDER BY `Kills` DESC");
            int count = 0;
            boolean done = false;
            while(rs2.next() && !(done)){
                count++;
                String nameUUID = rs2.getString("UUID");
                UUID uuid = UUID.fromString(nameUUID);
                if(uuid.equals(getUUID())){
                    done = true;
                    this.rank = count;
                }
            }
            rs2.close();
            rs.close();
            st.close();
            setReady(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataAsync() {
        SimpleStats.getSQLManager().getAsyncHandler().getExecutor().execute(new Runnable() {
            public void run() {
                PlayerStats.this.loadData();
            }
        });
    }
}