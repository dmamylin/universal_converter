module org.madbunny.converter.core {
    requires opencsv;
    requires slf4j.api;

    exports org.madbunny.converter.core.api;
    exports org.madbunny.converter.core.api.exceptions;
}
