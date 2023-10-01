package onlydust.com.marketplace.api.rest.api.adapter.exception;

public interface RestApiExceptionCode {

    String INVALID_JWT_FORMAT = "T.INVALID_JWT_FORMAT";
    String INVALID_JWT_SIGNATURE = "T.INVALID_JWT_SIGNATURE";
    String INVALID_JWT_ALGO_TYPE = "T.INVALID_JWT_ALGO_TYPE";
    String INVALID_JWT_HEADER_FORMAT = "T.INVALID_JWT_HEADER_FORMAT";
    String UNABLE_TO_DESERIALIZE_JWT_TOKEN = "T.UNABLE_TO_DESERIALIZE_JWT_TOKEN";
}
