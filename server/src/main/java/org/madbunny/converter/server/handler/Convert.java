package org.madbunny.converter.server.handler;

import com.google.gson.Gson;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import org.madbunny.converter.core.api.UnitsConverter;
import org.madbunny.converter.core.api.exceptions.ImpossibleToConvertException;
import org.madbunny.converter.core.api.exceptions.UnknownUnitsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Path("/convert")
public class Convert {
    private static final Logger LOG = LoggerFactory.getLogger(Convert.class);

    private static final int SIGNIFICANT_DIGITS = 15;
    private static final MathContext MATH_CONTEXT = new MathContext(SIGNIFICANT_DIGITS, RoundingMode.CEILING);

    private static final int RESPONSE_CODE_OK = 200;
    private static final int RESPONSE_CODE_UNKNOWN_UNITS = 400;
    private static final int RESPONSE_CODE_CANNOT_CONVERT = 404;
    private static final int RESPONSE_CODE_GENERAL_EXCEPTION = 500;

    private final UnitsConverter converter;
    private final Gson jsonFormatter = new Gson();

    private static class RequestBody {
        public final String from;
        public final String to;

        public RequestBody(String from, String to) {
            this.from = from;
            this.to = to;
        }

        // Gson fails without this constructor
        private RequestBody() {
            from = "";
            to = "";
        }
    }

    private static class UnknownUnitsErrorBody {
        public final String message;
        public final String[] unknownUnits;

        public UnknownUnitsErrorBody(UnknownUnitsException exception) {
            this.message = exception.getMessage();
            this.unknownUnits = exception.unknownUnits;
        }
    }

    private static class ImpossibleToConvertErrorBody {
        public final String message;
        public final String from;
        public final String to;

        public ImpossibleToConvertErrorBody(ImpossibleToConvertException exception) {
            this.message = exception.getMessage();
            this.from = exception.from;
            this.to = exception.to;
        }
    }

    private static class GeneralErrorBody {
        public final String message;

        public GeneralErrorBody(Exception exception) {
            this.message = exception.getMessage();
        }
    }

    public Convert(UnitsConverter converter) {
        this.converter = converter;
    }

    @POST
    public void doConvert(Context ctx) {
        try {
            var body = ctx.body(RequestBody.class);
            var result = converter.convert(body.from, body.to);
            onSuccess(ctx, result);
        } catch (UnknownUnitsException exception) {
            onUnknownUnits(ctx, exception);
        } catch (ImpossibleToConvertException exception) {
            onImpossibleToConvert(ctx, exception);
        } catch (Exception exception) {
            onGeneralException(ctx, exception);
        }
    }

    private static void onSuccess(Context ctx, BigDecimal result) {
        ctx.setResponseType(MediaType.TEXT).setResponseCode(RESPONSE_CODE_OK).send(formatResult(result));
    }

    private static String formatResult(BigDecimal result) {
        var truncated = new BigDecimal(result.toPlainString(), MATH_CONTEXT);
        return truncated.stripTrailingZeros().toPlainString();
    }

    private void onUnknownUnits(Context ctx, UnknownUnitsException exception) {
        var body = new UnknownUnitsErrorBody(exception);
        ctx.setResponseType(MediaType.JSON).setResponseCode(RESPONSE_CODE_UNKNOWN_UNITS).send(formatError(body));
    }

    private void onImpossibleToConvert(Context ctx, ImpossibleToConvertException exception) {
        var body = new ImpossibleToConvertErrorBody(exception);
        ctx.setResponseType(MediaType.JSON).setResponseCode(RESPONSE_CODE_CANNOT_CONVERT).send(formatError(body));
    }

    private void onGeneralException(Context ctx, Exception exception) {
        var body = new GeneralErrorBody(exception);
        ctx.setResponseType(MediaType.JSON).setResponseCode(RESPONSE_CODE_GENERAL_EXCEPTION).send(formatError(body));
    }

    private <Body> String formatError(Body body) {
        return jsonFormatter.toJson(body);
    }
}
