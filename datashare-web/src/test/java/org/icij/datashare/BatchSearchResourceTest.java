package org.icij.datashare;

import net.codestory.http.WebServer;
import net.codestory.http.misc.Env;
import net.codestory.rest.FluentRestTest;
import net.codestory.rest.Response;
import org.icij.datashare.batch.BatchSearch;
import org.icij.datashare.batch.BatchSearchRepository;
import org.icij.datashare.batch.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.icij.datashare.text.Project.project;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BatchSearchResourceTest implements FluentRestTest {
    @Mock BatchSearchRepository batchSearchRepository;
    private static WebServer server = new WebServer() {
            @Override
            protected Env createEnv() {
                return Env.prod();
            }
        }.startOnRandomPort();

    @Test
    public void test_upload_batch_search_csv_with_bad_parts_number() throws SQLException {
        when(batchSearchRepository.save(any(), any())).thenReturn(true);

        postRaw("/api/batch/search/prj", "multipart/form-data;boundary=AaB03x", "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"name\"\r\n" +
                "\r\n" +
                "value\r\n" +
                "--AaB03x--").
                should().respond(400);
    }

    @Test
        public void test_upload_batch_search_csv_with_bad_names() throws SQLException {
        when(batchSearchRepository.save(any(), any())).thenReturn(true);

        postRaw("/api/batch/search/prj", "multipart/form-data;boundary=AaB03x", "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"name\"\r\n" +
                "\r\n" +
                "my batch search\r\n" +
                "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"desc\"\r\n" +
                "\r\n" +
                "search description\r\n" +
                "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"files\"; filename=\"search.csv\"\r\n" +
                "\r\n" +
                "query\r\n" +
                "--AaB03x--").
                should().respond(400);
    }

    @Test
    public void test_upload_batch_search_csv() throws SQLException {
        when(batchSearchRepository.save(any(), any())).thenReturn(true);

        Response response = postRaw("/api/batch/search/prj", "multipart/form-data;boundary=AaB03x", "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"name\"\r\n" +
                "\r\n" +
                "my batch search\r\n" +
                "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"description\"\r\n" +
                "\r\n" +
                "search description\r\n" +
                "--AaB03x\r\n" +
                "Content-Disposition: form-data; name=\"search\"; filename=\"search.csv\"\r\n" +
                "Content-Type: text/csv\r\n" +
                "\r\n" +
                "query one\r\n" +
                "query two\r\n" +
                "query three\r\n" +
                "--AaB03x--").response();

        assertThat(response.code()).isEqualTo(200);
        verify(batchSearchRepository).save(any(), eq(new BatchSearch(response.content(),
                project("prj"), "my batch search", "search description",
                asList("query one", "query two", "query three"), new Date(), BatchSearch.State.RUNNING)));
    }

    @Test
    public void test_get_search_results_json() throws Exception {
        when(batchSearchRepository.getResults("batchSearchId")).thenReturn(asList(
                new SearchResult("docId1", "rootId1", Paths.get("/path/to/doc1"), new Date(), 1),
                new SearchResult("docId2", "rootId2", Paths.get("/path/to/doc2"), new Date(), 2)
        ));

        get("/api/batch/search/result/batchSearchId").
                should().respond(200).haveType("application/json").
                contain("\"documentId\":\"docId1\"").
                contain("\"documentId\":\"docId2\"");
    }

    @Test
    public void test_get_search_results_csv() throws Exception {
        when(batchSearchRepository.getResults("batchSearchId")).thenReturn(asList(
                new SearchResult("docId1", "rootId1", Paths.get("/path/to/doc1"), new Date(), 1),
                new SearchResult("docId2", "rootId2", Paths.get("/path/to/doc2"), new Date(), 2)
        ));

        get("/api/batch/search/result/csv/batchSearchId").
                should().respond(200).haveType("text/csv").
                haveHeader("Content-Disposition", "attachment;filename=\"batchSearchId.csv\"").
                contain("\"docId1\",\"rootId1\"").
                contain("\"docId2\",\"rootId2\"");
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        server.configure(routes -> routes.add(new BatchSearchResource(batchSearchRepository)));
    }

    @Override public int port() { return server.port();}
}