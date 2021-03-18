package org.madbunny.converter.server;

import com.google.gson.Gson;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ServerOptions;
import org.madbunny.converter.core.api.UnitsConverter;
import org.madbunny.converter.core.api.UnitsConverterFactory;
import org.madbunny.converter.core.api.UnitsDatabase;
import org.madbunny.converter.core.api.UnitsDatabaseFactory;
import org.madbunny.converter.server.handler.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Server {
    static private final Logger LOG = LoggerFactory.getLogger(Server.class);
    static private final int DEFAULT_PORT = 80;

    public static void main(final String[] args) {
        Jooby.runApp(getJoobyArguments(args), server -> {
            var unitsDatabase = createUnitsDatabase(args);
            var converter = createUnitsConverter(unitsDatabase);
            var jsonParser = new Gson();
            var serverOptions = createServerOptions();

            server.setServerOptions(serverOptions);
            server.decoder(MediaType.json, (ctx, type) -> {
                var body = ctx.body().value();
                return jsonParser.fromJson(body, type);
            });
            server.mvc(new Convert(converter));
            LOG.info(String.format("Starting the universal converter server on port: %d", serverOptions.getPort()));
        });
    }

    private static UnitsDatabase createUnitsDatabase(String[] args) {
        var dbFileName = getDatabaseFileName(args);
        UnitsDatabase db = null;
        try {
            db = UnitsDatabaseFactory.createFromCsvFile(dbFileName);
        } catch (Exception exception) {
            onStartupError(exception.getMessage());
        }
        return db;
    }

    private static UnitsConverter createUnitsConverter(UnitsDatabase unitsDatabase) {
        UnitsConverter converter = null;
        try {
            converter = UnitsConverterFactory.createOverDb(unitsDatabase);
        } catch (Exception exception) {
            onStartupError(exception.getMessage());
        }
        return converter;
    }

    private static String getDatabaseFileName(String[] args) {
        if (args.length == 0) {
            onStartupError("Path to data file is not provided");
        }
        return args[0];
    }

    private static ServerOptions createServerOptions() {
        return new ServerOptions()
                .setPort(DEFAULT_PORT);
    }

    private static String[] getJoobyArguments(String[] args) {
        var begin = Math.min(1, args.length);
        return Arrays.copyOfRange(args, begin, args.length);
    }

    private static void onStartupError(String message) {
        LOG.error(message);
        throw new RuntimeException(message);
    }
}
