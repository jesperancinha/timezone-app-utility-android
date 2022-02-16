package org.jesperancinha.google.api;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

class GoogleAPITest {

    @Test
    void getTimeZone() throws Exception {

        DateTimeZone timeZone = GoogleAPI.getTimeZone("Amsterdam", "NL");

        assert timeZone == DateTimeZone.UTC;
    }
}