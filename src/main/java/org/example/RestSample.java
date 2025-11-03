package org.example;

import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * CODEMIND REST api example code
 *      - login
 *      - analyze
 *      - check analysis status
 *      - analysis result
 *      - checkLogin
 *  테스트 전 코드마인드에 프로젝트(ex. kr.codemind.lab)가 등록 되어있어야 합니다. (git or 소스경로지정)
 *  아래 서버 URL 및 인증정보 그리고 프로젝트 명을 기입 한 후 실행하면 됩니다.
 */
public class RestSample {
    static CloseableHttpClient httpclient;
    static final int REQUEST_TIMEOUT = 3600;        // 1 hour
    static final String CODEMIND_URL = "http://10.0.1.123:8083";
    static final String USERNAME = "openapi";
    static final String PASSWORD = "codemind@2";
    static final String PROJECT_NAME = "project_1";

    static String csrf = "";

    private static void createHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(), NoopHostnameVerifier.INSTANCE);

        httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .build();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        createHttpClient();

        login();

        checkLogin();

        analyze();

        int seq = checkAnal();

        loadAnalysisResult(seq);

        loadAnalysisResultRuleStatistics(seq);

        addProject();
        updateProject();
        deleteProject();
    }

    /**
     * 로그인
     */
    private static void login() throws IOException {
        HttpPost post = new HttpPost(CODEMIND_URL + "/user/login/process");
        List<NameValuePair> entity = new ArrayList<>();
        entity.add(new BasicNameValuePair("REQUEST_KIND", "API"));
        entity.add(new BasicNameValuePair("username", USERNAME));
        entity.add(new BasicNameValuePair("password", PASSWORD));
        post.setEntity(new UrlEncodedFormEntity(entity));

        try (CloseableHttpResponse response = httpclient.execute(post)) {
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (content.startsWith("CM-")) {
                    // application fail
                    throw new LoginException("login failed: " + content);
                } else {
                    System.out.println("login successfully");
                }
            } else {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), content);
            }
        } catch (Exception ex) {
            System.out.println("login: " + ex.getMessage());
        }
    }

    /**
     * 프로젝트 분석 요청
     */
    private static void analyze() throws IOException {
        HttpPost post = new HttpPost(CODEMIND_URL + "/api/analysis/" + RestSample.PROJECT_NAME);      // 현재 API
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                JSONObject json = new JSONObject(content);
                throw new InvalidParameterException(json.getString("message"));
            } else {
                System.out.println("start analyzing...");
            }
        }
    }

    /**ㅓㅓㅓ
     * 프로젝트 분석 진행상태 체크djfkal;jfdklajseohn;alkhv;ael;hb;hio;aefjkdlwobnagorih
     */
    private static int checkAnal() throws IOException {
        CloseableHttpResponse response = null;
        int sequence = 0;
        try {
            int count = 0;
            while(true) {
                if( count >= REQUEST_TIMEOUT ) {
                    System.out.printf("[%s] analysis failed: request timeout %s sec.%n", RestSample.PROJECT_NAME, count);
                    break;
                }
                HttpGet get = new HttpGet(CODEMIND_URL + "/api/" + RestSample.PROJECT_NAME + "/status");
                response = httpclient.execute(get);
                String content = new BasicResponseHandler().handleEntity(response.getEntity());
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    JSONObject json = new JSONObject(content);
                    String status = json.getString("status");
                    if (status.equals("success")) {
                        // success
                        System.out.println("analysis successfully");
                        sequence = Integer.parseInt(Optional.ofNullable(json.getString("sequence")).orElse("0"));       // numOfReports > sequence 로 변경 - 23/03/07
                        break;
                    } else if (isRunning(status)) {     // 진행중 상태체크 변경 - 23/03/09
                        // noop
                        System.out.println(status + "..." + count);
                    } else {
                        // analysis failed
                        System.out.println("analysis failed");
                        break;
                    }
                } else {
                    throw new InvalidParameterException(content);
                }

                count += 5;
                TimeUnit.SECONDS.sleep(5);
            }
            System.out.println(count);
        } catch (InterruptedException e) {
            // noop
        } finally {
            if(response != null) response.close();
        }
        return sequence;
    }

    /**
     * 진행중인 상태 체크
     */
    private static boolean isRunning(String status) {
        return !status.equals("success") && !status.equals("stop") && !status.equals("fail") && !status.isEmpty();
    }

    /**
     * 프로젝트 분석 결과 조회
     */
    private static void loadAnalysisResult(int sequence) throws IOException {
        String url = CODEMIND_URL + "/api/" + RestSample.PROJECT_NAME + "/" + sequence + "/analysis-result";
        System.out.println("URL: " + url);
        HttpGet get = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(get);
        String content = new BasicResponseHandler().handleEntity(response.getEntity());
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            JSONObject json = new JSONObject(content);
            // 조회된 결과에는 아래와 같이 분석결과 요약정보 및 각 파일에 상세 취약점 정보가 포함됩니다.
            // warns: [] - 취약점 목록, files: {} - 파일 목록, canons: [] - 규칙 목록
            System.out.println("------------------------------------------------------------------------------");
            System.out.println("project: " + json.getString("project") +
                    ", totalLines: " + json.getInt("totalLines") +
                    ", files: " + json.getJSONObject("files").length() +
                    ", vulnerability: " + json.getJSONArray("warns").length() +
                    ", startTime: " + json.getInt("startTime") +
                    ", endTime: " + json.getInt("endTime"));
        } else {
            throw new InvalidParameterException(content);
        }
    }

    /**
     * 프로젝트 분석 결과 조회
     */
    private static void loadAnalysisResultRuleStatistics(int sequence) throws IOException {
        String url = CODEMIND_URL + "/api/" + RestSample.PROJECT_NAME + "/" + sequence + "/analysis-result-rule-statistics";
        System.out.println("URL: " + url);
        HttpGet get = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(get);
        String content = new BasicResponseHandler().handleEntity(response.getEntity());
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            JSONObject json = new JSONObject(content);
            // 조회된 결과에는 아래와 같이 분석결과 요약정보 및 각 파일에 상세 취약점 정보가 포함됩니다.
            // warns: [] - 취약점 목록, files: {} - 파일 목록, canons: [] - 규칙 목록
            System.out.println("------------------------------------------------------------------------------");
            System.out.println("project: " + json.getString("project_name") +
                    ", totalLines: " + json.getInt("total_lines") +
                    ", files: " + json.getInt("total_files") +
                    ", canons: " + json.getJSONArray("canons").length() +
                    ", startTime: " + json.getInt("start_time") +
                    ", endTime: " + json.getInt("end_time"));
        } else {
            throw new InvalidParameterException(content);
        }
    }

    /**
     * /user/login/check
     */
    private static void checkLogin() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet get = new HttpGet(CODEMIND_URL + "/user/login/check");
            get.setHeader("Referer", "http://127.0.0.1:8080/user/login/process");       // value 값에 아무 URL 을 전달하면 됩니다.
            System.out.println("Referer: " + get.getFirstHeader("Referer"));
            response = httpclient.execute(get);
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONObject json = new JSONObject(content);
                csrf = json.getString("_csrf");
                System.out.println("loginCheck Response: " + content);
            } else {
                throw new InvalidParameterException(content);
            }
        } catch (Exception e) {
            System.out.println("checkLogin: " + e.getMessage());
        } finally {
            if(response != null) response.close();
        }
    }

    private static void addProject() throws IOException {
        HttpPost post = new HttpPost(CODEMIND_URL + "/api/project/create");
        CloseableHttpResponse response = null;
        try {
            List<NameValuePair> entity = getNameValuePairs();
            post.setEntity(new UrlEncodedFormEntity(entity));
            post.setHeader("Referer", "http://10.0.0.23:8080");
            response = httpclient.execute(post);
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("project create failed: " + content);
            }
            else {
                System.out.println("project created ok...");
            }
        } finally {
            if(response != null) response.close();
        }
    }

    private static List<NameValuePair> getNameValuePairs() {
        List<NameValuePair> entity = new ArrayList<>();
        entity.add(new BasicNameValuePair("name", "PJ00001"));
        entity.add(new BasicNameValuePair("title", "sample project by RestSample"));
        entity.add(new BasicNameValuePair("repoType", "git"));
        entity.add(new BasicNameValuePair("repoPath", "https://github.com/TheAlgorithms/Java.git"));
        entity.add(new BasicNameValuePair("branch", "master"));
        entity.add(new BasicNameValuePair("repoId", ""));
        entity.add(new BasicNameValuePair("repoPw", ""));
        entity.add(new BasicNameValuePair("buildEnvId", "2"));
        entity.add(new BasicNameValuePair("ruleset_list", "1,2,3"));
        entity.add(new BasicNameValuePair("analTimeout", "0"));
        entity.add(new BasicNameValuePair("equalizer", "100/50/100/100"));
        entity.add(new BasicNameValuePair("_csrf", csrf));
        return entity;
    }

    private static void updateProject() throws IOException {
        HttpPut put = new HttpPut(CODEMIND_URL + "/api/project/PJ00001/update");
        CloseableHttpResponse response = null;
        try {
            // 삼성화재 요청한 parameters: title, branch, analTimeout, equalizer, _csrf, projectName, buildEnvId, repoType, repoPath, rulesetList
            List<NameValuePair> entity = getValuePairs();
            put.setEntity(new UrlEncodedFormEntity(entity));
            response = httpclient.execute(put);
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("project update failed: " + content);
            }
            else {
                System.out.println("project update ok...");
            }
        } finally {
            if(response != null) response.close();
        }
    }

    private static List<NameValuePair> getValuePairs() {
        List<NameValuePair> entity = new ArrayList<>();
        entity.add(new BasicNameValuePair("title", "sample project modified by RestSample"));
        entity.add(new BasicNameValuePair("repoType", "git"));
        entity.add(new BasicNameValuePair("repoPath", "https://github.com/TheAlgorithms/Java.git"));
        entity.add(new BasicNameValuePair("branch", "master"));
        entity.add(new BasicNameValuePair("buildEnvId", "2"));
        entity.add(new BasicNameValuePair("rulesetList", "1,2,3"));
        return entity;
    }

    private static void deleteProject() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet get = new HttpGet(CODEMIND_URL + "/api/project/PJ00001/delete");
            response = httpclient.execute(get);
            String content = new BasicResponseHandler().handleEntity(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                System.out.println("project delete ok...");
            } else {
                System.out.println("project delete failed: " + content);
            }
        } finally {
            if(response != null) response.close();
        }
    }
}


