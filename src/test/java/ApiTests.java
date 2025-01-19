import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.poi.ss.usermodel.*;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static java.net.URLEncoder.encode;


public class ApiTests {

    String baseURL1 = "https://restcountries.com/v3.1/all";

    @BeforeTest
    public void setup() {
        RestAssured.baseURI = baseURL1;
    }

    @Test(priority = 1)
    public void testValidCountryStatus() {
        String countryName = "Germany";

        // Correct API URL for a valid country (Germany)
        Response response = RestAssured
                .given()
                .when()
                .get("/name/" + countryName.toLowerCase());

        logResponse(response);
        Assert.assertEquals(response.getStatusCode(), 200, "Status code mismatch!");
        Assert.assertTrue(response.asString().contains("Germany"), "Country name not found in the response!");
    }
    @Test
    public void testInvalidCountry() {
        String countryName = "NonExistentCountry";
        // URL for a country that doesn't exist (invalid country)
        Response response = RestAssured
                .given()
                .when()
                .get("/name/" + countryName);

        logResponse(response);
        Assert.assertEquals(response.getStatusCode(), 404, "Expected 404 for an invalid country!");
    }

    @DataProvider(name = "countryDataProvider")
    public Object[][] countryDataProvider() throws IOException {
        String filePath = "src/test/java/CountryDataProvider.xlsx";
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        int rowCount = sheet.getPhysicalNumberOfRows();
        Object[][] data = new Object[rowCount - 1][2]; // Exclude header row

        for (int i = 1; i < rowCount; i++) {
            Row row = sheet.getRow(i);

            if (row == null) {
                throw new IllegalArgumentException("Row " + i + " is null in the Excel file.");
            }

            Cell countryCell = row.getCell(0);
            Cell statusCodeCell = row.getCell(1);

            if (countryCell == null || countryCell.getCellType() != CellType.STRING) {
                throw new IllegalArgumentException("Invalid or missing country name at row " + i);
            }
            if (statusCodeCell == null || statusCodeCell.getCellType() != CellType.NUMERIC) {
                throw new IllegalArgumentException("Invalid or missing status code at row " + i);
            }

            String countryName = countryCell.getStringCellValue();
            int expectedStatusCode = (int) Math.round(statusCodeCell.getNumericCellValue());

            data[i - 1][0] = countryName;
            data[i - 1][1] = expectedStatusCode;

            // Debugging logs
            System.out.println("Loaded Country: " + countryName + ", Expected Status: " + expectedStatusCode);
        }

        workbook.close();
        fis.close();
        return data;
    }

    @Test(dataProvider = "countryDataProvider")
    public void testWithMultipleCountries(String countryName, int expectedStatusCode) {
        RestAssured.baseURI = "https://restcountries.com/v3.1";

        String encodedCountryName = URLEncoder.encode(countryName, StandardCharsets.UTF_8);

        // Log the request URL
        System.out.println("Request URL: " + RestAssured.baseURI + "/name/" + encodedCountryName);

        // Send API request
        Response response = RestAssured
                .given()
                .when()
                .get("/name/" + encodedCountryName);

        // Log the response
        System.out.println("Response for " + countryName + ": " + response.asString());
        System.out.println("Status code: " + response.getStatusCode());

        // Assert the status code
        Assert.assertEquals(response.getStatusCode(), expectedStatusCode,
                "Status code mismatch for country: " + countryName);

        // Additional check for valid countries
        if (expectedStatusCode == 200) {
            Assert.assertTrue(response.asString().contains(countryName),
                    "Country name not found in the response!");
        }
    }
    /**
     * Performance testing: Measure response time.
     */
    @Test
    public void testPerformance() {
        Response response = RestAssured
                .given()
                .when()
                .get("/all");

        logResponse(response);
        long responseTime = response.timeIn(TimeUnit.MILLISECONDS);
        System.out.println("Response time: " + responseTime + " ms");
        Assert.assertTrue(responseTime < 2000, "API response time is too high!");
    }

    /**
     * Helper method to log response details.
     */
    private void logResponse(Response response) {
        System.out.println("\n--- API Response ---");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.prettyPrint());
        System.out.println("--- End of Response ---\n");
    }
}