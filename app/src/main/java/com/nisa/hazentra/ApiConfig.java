package com.nisa.hazentra;

public final class ApiConfig {

    private ApiConfig() {

    }

    public static final String BASE_URL =
            "https://garment-friday-parasail.ngrok-free.dev";

    public static final String GET_HAZARDS =
            BASE_URL
                    + "/hazentra/api/get_hazards.php";

    public static final String ADD_HAZARD =
            BASE_URL
                    + "/hazentra/api/add_hazard.php";
}