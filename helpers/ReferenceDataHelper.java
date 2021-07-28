package org.collegeboard.apfym.helpers;

import static org.collegeboard.apfym.constants.Cost.setCostValuesBasedOnEPC;
import static org.collegeboard.apfym.helpers.ApfymRestHelper.ENTERPRISE_REFERENCE_DATA;
import static org.collegeboard.apfym.helpers.ApfymRestHelper.MSAP_REFERENCE_POLL_LAMBDA;
import static org.collegeboard.apfym.helpers.ApfymRestHelper.MSREFERENCEPOLL_LAMBDA_PAYLOAD_VALID;
import static org.collegeboard.apfym.helpers.ApfymRestHelper.getExamWindowForEdPeriod;

import java.io.IOException;
import java.util.HashMap;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.cb.raf.awsutil.DynamoData;
import org.cb.raf.awsutil.LambdaClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;


public class ReferenceDataHelper
{
    private static final LambdaClient LAMBDA_CLIENT = LambdaClient.getInstance();
    private static final DynamoData DYNAMO_DB = DynamoData.getInstance();
    private static final String CURRENT_ED_PD_URL = "reference/education-periods/current";
    private static final ExamOrderHelper EXAM_ORDER_HELPER = new ExamOrderHelper();
    private static final EnrollmentHelper ENROLLMENT_HELPER = new EnrollmentHelper();
    private static final SectionHelper SECTION_HELPER = new SectionHelper();
    private static final ProfCertHelper PROF_CERT_HELPER = new ProfCertHelper();
    private static final PaaproHelper PAAPRO_HELPER = new PaaproHelper();
    private static final CommonHelper COMMON_HELPER = new CommonHelper();
    private static final String ED_PERIODS_URL = "reference/education-periods";

    /**
     * + 1.Calculates the values for fields academicYrStartDt, academicYrEndDt, apAdminYr, educationPeriodEndDt,
     * educationPeriodStartDt based on educationPeriodCd in the input 2.Sets the new value for current education period
     * in reference data dynamo table. 3.Clears the cache for bsapenrollment, bsaporder, bsapsection, bsaprofcerts
     */

    public static void setCurrentEducationPeriodInReferenceTable(String educationPeriodCd) throws IOException
    {
        if (educationPeriodCd.isEmpty())
        {
            return;
        }
        int fullEdPdYr = Integer.parseInt("20" + educationPeriodCd);
        String academicYrStartDt = (fullEdPdYr - 2) + "-06-01T00:00:00.000Z";
        String academicYrEndDt = (fullEdPdYr - 1) + "-06-30T00:00:00.000Z";
        String apAdminYr = String.valueOf(fullEdPdYr - 1);
        String educationPeriodEndDt = (fullEdPdYr - 1) + "-07-31T00:00:00.000Z";
        String educationPeriodStartDt = (fullEdPdYr - 2) + "-06-01T00:00:00.000Z";
        setCurrentEdPdInReferenceTable(educationPeriodCd, academicYrStartDt, academicYrEndDt, apAdminYr,
            educationPeriodEndDt, educationPeriodStartDt);

        /*
            Exam costs are variable and depend on the current education period.
         */

        setCostValuesBasedOnEPC(educationPeriodCd);
        getExamWindowForEdPeriod(Integer.parseInt(educationPeriodCd));
    }

    public static void setCurrentEducationPeriodInReferenceTableInAllEdPd(String educationPeriodCd,
        String academicYrStartDt, String academicYrEndDt, String apAdminYr, String educationPeriodEndDt,
        String educationPeriodStartDt) throws IOException
    {
        String currentEdPd = DYNAMO_DB.getItemFromTable(ENTERPRISE_REFERENCE_DATA, "url", CURRENT_ED_PD_URL).toJSON();
        JSONObject responseObj = (new JSONObject(currentEdPd)).getJSONObject("response");
        JSONObject rootObj = responseObj.getJSONArray("cdRefList").getJSONObject(0);
        JSONArray arr = rootObj.getJSONArray("attributes");

        rootObj.put("code", educationPeriodCd);
        rootObj.put("descr",
            "20" + (Integer.parseInt(educationPeriodCd) - 2) + "-20" + (Integer.parseInt(educationPeriodCd) - 1));
        rootObj.put("sortOrder", Integer.parseInt(educationPeriodCd));

        for (int i = 0; i < arr.length(); i++)
        {
            String name = arr.getJSONObject(i).getString("name");
            if (name.equals("education_period_cd"))
            {
                changeObjValue(educationPeriodCd, arr, i, false);
            }
            if (name.equals("academic_yr_start_dt"))
            {
                changeObjValue(academicYrStartDt, arr, i, true);
            }
            if (name.equals("academic_yr_end_dt"))
            {
                changeObjValue(academicYrEndDt, arr, i, true);
            }
            if (name.equals("ap_admin_yr"))
            {
                changeObjValue(apAdminYr, arr, i, false);
            }
            if (name.equals("education_period_end_dt"))
            {
                changeObjValue(educationPeriodEndDt, arr, i, false);
            }
            if (name.equals("education_period_start_dt"))
            {
                changeObjValue(educationPeriodStartDt, arr, i, false);
            }
        }

        updateDynamoTable(responseObj.toString(), ED_PERIODS_URL);
        EXAM_ORDER_HELPER.clearCache();
        ENROLLMENT_HELPER.clearCache();
        SECTION_HELPER.clearCache();
        PROF_CERT_HELPER.clearCache();
        PAAPRO_HELPER.clearCache();
        COMMON_HELPER.clearCache();
        System.out.println(
            "Successfully updated the reference data dynamo db with education period - " + educationPeriodCd +
                "ED_PERIODS_URL -" + ED_PERIODS_URL);
    }

    /**
     * + resets the current education period in reference data dynamo table to defaults value (current)  by invoking the
     * reference poll lambda
     */
    public static void resetCurrentEducationPeriodInReferenceTable()
    {

          DYNAMO_DB.deleteItemInTable(ENTERPRISE_REFERENCE_DATA, "url", CURRENT_ED_PD_URL);
          DYNAMO_DB.deleteItemInTable(ENTERPRISE_REFERENCE_DATA, "url", ED_PERIODS_URL);
        InvokeResult lambdaResult =
            LAMBDA_CLIENT.invokeLambda(MSAP_REFERENCE_POLL_LAMBDA, MSREFERENCEPOLL_LAMBDA_PAYLOAD_VALID);
        Assert.assertNotNull(lambdaResult);
        Assert.assertNull(lambdaResult.getFunctionError());
        EXAM_ORDER_HELPER.clearCache();
        ENROLLMENT_HELPER.clearCache();
        SECTION_HELPER.clearCache();
        PROF_CERT_HELPER.clearCache();
        PAAPRO_HELPER.clearCache();
        COMMON_HELPER.clearCache();

        getExamWindowForEdPeriod(PAAPRO_HELPER.getCurrentYearDataOrgAgnosticWay(HttpStatus.SC_OK).getInt("code"));
    }

    private static void setCurrentEdPdInReferenceTable(String educationPeriodCd, String academicYrStartDt,
        String academicYrEndDt, String apAdminYr, String educationPeriodEndDt, String educationPeriodStartDt)
        throws IOException
    {
        String currentEdPd = DYNAMO_DB.getItemFromTable(ENTERPRISE_REFERENCE_DATA, "url", CURRENT_ED_PD_URL).toJSON();
        JSONObject responseObj = (new JSONObject(currentEdPd)).getJSONObject("response");
        JSONObject rootObj = responseObj.getJSONArray("cdRefList").getJSONObject(0);
        JSONArray arr = rootObj.getJSONArray("attributes");

        rootObj.put("code", educationPeriodCd);
        rootObj.put("descr",
            "20" + (Integer.parseInt(educationPeriodCd) - 2) + "-20" + (Integer.parseInt(educationPeriodCd) - 1));
        rootObj.put("sortOrder", Integer.parseInt(educationPeriodCd));

        for (int i = 0; i < arr.length(); i++)
        {
            String name = arr.getJSONObject(i).getString("name");
            if (name.equals("education_period_cd"))
            {
                changeObjValue(educationPeriodCd, arr, i, false);
            }
            if (name.equals("academic_yr_start_dt"))
            {
                changeObjValue(academicYrStartDt, arr, i, false);
            }
            if (name.equals("academic_yr_end_dt"))
            {
                changeObjValue(academicYrEndDt, arr, i, false);
            }
            if (name.equals("ap_admin_yr"))
            {
                changeObjValue(apAdminYr, arr, i, false);
            }
            if (name.equals("education_period_end_dt"))
            {
                changeObjValue(educationPeriodEndDt, arr, i, false);
            }
            if (name.equals("education_period_start_dt"))
            {
                changeObjValue(educationPeriodStartDt, arr, i, false);
            }
        }
        if (!((new JSONObject(currentEdPd)).getJSONObject("response").getJSONArray("cdRefList").getJSONObject(0))
            .toString().equals(rootObj.toString()))
        {
            updateDynamoTable(responseObj.toString(), CURRENT_ED_PD_URL);
            EXAM_ORDER_HELPER.clearCache();
            ENROLLMENT_HELPER.clearCache();
            SECTION_HELPER.clearCache();
            PROF_CERT_HELPER.clearCache();
            PAAPRO_HELPER.clearCache();
            COMMON_HELPER.clearCache();
            System.out.println(
                "Successfully updated the reference data dynamo db with education period - " + educationPeriodCd);
            return;
        }
        System.out.println("No change in education period in the input. Reference data dynamo db is not updated");
    }

    private static void changeObjValue(String input, JSONArray arr, int index, boolean compareDate)
    {
        boolean valueChanged = false;
        if (input != null)
        {
            String actualValue = arr.getJSONObject(index).getString("value");
            String valueToBeSaved = input;
            String existingValue = actualValue;
            if (compareDate && actualValue.contains("T"))
            {
                existingValue = actualValue.split("T")[0];
                valueToBeSaved = input + "T" + actualValue.split("T")[1];
            }
            if (!existingValue.equals(input))
            {
                valueChanged = true;
            }
            if (valueChanged)
            {
                arr.getJSONObject(index).put("value", valueToBeSaved);
            }
        }
    }

    private static void updateDynamoTable(String val, String urlString) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>()
        {
        };

        HashMap<String, Object> map = mapper.readValue(val, typeRef);
        Item item = new Item().withPrimaryKey("url", urlString).withMap("response", map);
        DYNAMO_DB.getTable(ENTERPRISE_REFERENCE_DATA).putItem(item);

    }

    public String getAllEducationPeriodsFromEnterpriseRefData()
    {
        return DYNAMO_DB.getItemFromTable(ENTERPRISE_REFERENCE_DATA, "url", ED_PERIODS_URL).toJSON();
    }

    public String getCurrentEducationPeriodFromEnterpriseRefData()
    {
        return DYNAMO_DB.getItemFromTable(ENTERPRISE_REFERENCE_DATA, "url", CURRENT_ED_PD_URL).toJSON();
    }

    public int getCurrentEducationPeriodCode()
    {
        final String refData = getCurrentEducationPeriodFromEnterpriseRefData();
        return new JSONObject(refData)
            .getJSONObject("response")
            .getJSONArray("cdRefList")
            .getJSONObject(0)
            .getInt("code");
    }
}