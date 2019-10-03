package org.icij.datashare.tasks;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.batch.BatchSearch;
import org.icij.datashare.batch.BatchSearchRepository;
import org.icij.datashare.test.ElasticsearchRule;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.indexing.elasticsearch.ElasticsearchIndexer;
import org.icij.datashare.text.nlp.Pipeline;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.icij.datashare.test.ElasticsearchRule.TEST_INDEX;
import static org.icij.datashare.text.Document.Status.DONE;
import static org.icij.datashare.text.Language.FRENCH;
import static org.icij.datashare.text.Project.project;
import static org.icij.datashare.user.User.local;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BatchSearchRunnerIntTest {
    @ClassRule
    public static ElasticsearchRule es = new ElasticsearchRule();
    private ElasticsearchIndexer indexer = new ElasticsearchIndexer(es.client, new PropertiesProvider()).withRefresh(IMMEDIATE);
    @Mock BatchSearchRepository repository;
    @After public void tearDown() throws IOException { es.removeAll();}

    @Test
    public void test_search_with_file_types() throws Exception {
        Document mydoc = createDoc("mydoc");
        indexer.add(TEST_INDEX, mydoc);
        BatchSearch searchKo = new BatchSearch(project(TEST_INDEX), "name", "desc", singletonList("mydoc"), false, singletonList("application/pdf"), null);
        BatchSearch searchOk = new BatchSearch(project(TEST_INDEX), "name", "desc", singletonList("mydoc"), false, singletonList("text/plain"), null);
        when(repository.getQueued()).thenReturn(asList(searchKo, searchOk));

        new BatchSearchRunner(indexer, repository, local()).call();

        verify(repository, never()).saveResults(eq(searchKo.uuid), eq("mydoc"), anyList());
        verify(repository).saveResults(searchOk.uuid, "mydoc", singletonList(mydoc));
    }

    @Test
    public void test_search_with_paths() throws Exception {
        Document mydoc = createDoc("mydoc");
        indexer.add(TEST_INDEX, mydoc);
        BatchSearch searchKo = new BatchSearch(project(TEST_INDEX), "name", "desc", singletonList("mydoc"), false, null,
                singletonList("/foo/bar"));
        BatchSearch searchOk = new BatchSearch(project(TEST_INDEX), "name", "desc", singletonList("mydoc"), false, null,
                singletonList("file:///path/to"));
        when(repository.getQueued()).thenReturn(asList(searchKo, searchOk));

        new BatchSearchRunner(indexer, repository, local()).call();

        verify(repository, never()).saveResults(eq(searchKo.uuid), eq("mydoc"), anyList());
        verify(repository).saveResults(searchOk.uuid, "mydoc", singletonList(mydoc));
    }

    private Document createDoc(String name, Pipeline.Type... pipelineTypes) {
        return new Document(project("prj"), name, Paths.get("/path/to/").resolve(name), "content " + name,
                FRENCH, Charset.defaultCharset(),
                "text/plain", new HashMap<>(), DONE,
                Arrays.stream(pipelineTypes).collect(toSet()), new Date(),
                null, null, 0, 123L);
    }

    @Before
    public void setUp() { initMocks(this);}
}
