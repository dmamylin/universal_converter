module org.madbunny.converter.server {
    requires org.madbunny.converter.core;
    requires jooby;
    requires com.google.gson;
    requires slf4j.api;
    requires org.apache.logging.log4j;

    opens org.madbunny.converter.server.handler;
}
