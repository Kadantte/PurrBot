package net.andre601.util;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import net.andre601.core.PurrBotMain;

import java.util.Map;

public class DBUtil {

    public static final RethinkDB r = RethinkDB.r;

    public static Connection con = r.connection().hostname(PurrBotMain.file.getItem("config", "db-ip"))
            .port(28015).db((PurrBotMain.file.getItem("config", "beta").equalsIgnoreCase("true") ?
                    "beta" : "main"
            )).connect();

    public static Map<String, Object> getPrefix(String guild){
        return r.table("guild_prefix").get(guild).run(con);
    }

    public static void resetPrefix(String guild){
        r.table("guild_prefix").get(guild).delete().run(con);
    }

    public static Map<String, Object> getWelcomeChannel(String guild){
        return r.table("guild_welcome").get(guild).run(con);
    }

    public static void resetWelcome(String guild){
        r.table("guild_welcome").get(guild).delete().run(con);
    }

}