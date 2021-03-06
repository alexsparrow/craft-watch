package watch.craft.executor

import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import watch.craft.*
import watch.craft.Scraper.Job
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.Job.More
import watch.craft.Scraper.ScrapedItem
import watch.craft.executor.ScraperAdapter.Result
import watch.craft.network.Retriever
import watch.craft.utils.textFrom
import java.net.URI

class ScraperAdapterTest {
  private val retriever = mock<Retriever> {
    onBlocking { retrieve(any(), any()) } doAnswer {
      "<html><body><h1>${it.getArgument<URI>(0)}</h1></body></html>".toByteArray()
    }
  }

  private val itemA = mock<ScrapedItem>()
  private val itemB = mock<ScrapedItem>()

  @Test
  fun `enriches item with results info`() {
    val adapter = adapterWithSingleLeaf { itemA }

    assertEquals(
      listOf(
        Result(breweryName = BREWERY_NAME, rawName = "A", url = URL_A, item = itemA)
      ),
      execute(adapter).entries
    )
  }

  @Test
  fun `passes correct URLs and HTML around`() {
    val work = mock<(Document) -> ScrapedItem> {
      on { invoke(any()) } doThrow SkipItemException("Emo town")
    }

    val adapter = adapter(listOf(
      Leaf(name = "A", url = URL_A, work = work)
    ))

    execute(adapter)

    verifyBlocking(retriever) { retrieve(URL_A, ".html") }
    verify(work)(docWithHeaderMatching(URL_A.toString()))
  }

  @Nested
  inner class Traversal {
    @Test
    fun `multiple flat items`() {
      val adapter = adapter(listOf(
        Leaf(name = "A", url = URL_A) { itemA },
        Leaf(name = "A", url = URL_B) { itemB }
      ))

      assertEquals(listOf(itemA, itemB), execute(adapter).items())
    }

    @Test
    fun `non-leaf node`() {
      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { itemA },
            Leaf(name = "A", url = URL_B) { itemB }
          )
        }
      ))

      assertEquals(listOf(itemA, itemB), execute(adapter).items())
    }

    @Test
    fun `multiple non-leaf nodes`() {
      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { itemA },
            More(url = PAGE_2_URL) {
              listOf(
                Leaf(name = "A", url = URL_B) { itemB }
              )
            }
          )
        }
      ))

      assertEquals(listOf(itemA, itemB), execute(adapter).items())
    }
  }

  @Nested
  inner class ErrorHandling {
    @Test
    fun `fatal exception during request kills everything`() {
      retriever.stub {
        onBlocking { retriever.retrieve(any(), any()) } doThrow FatalScraperException("Uh oh")
      }

      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { itemA },
            Leaf(name = "A", url = URL_B) { itemB }
          )
        }
      ))

      assertThrows<FatalScraperException> {
        execute(adapter)
      }
    }

    @Test
    fun `non-fatal exception during non-leaf scrape doesn't kill everything`() {
      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { itemA },
            More(url = PAGE_2_URL) { throw MalformedInputException("Uh oh") }
          )
        }
      ))

      assertEquals(listOf(itemA), execute(adapter).items())    // Other item is returned
    }

    @Test
    fun `non-fatal exception during leaf scrape doesn't kill everything`() {
      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { throw MalformedInputException("Uh oh") },
            Leaf(name = "A", url = URL_B) { itemB }
          )
        }
      ))

      assertEquals(listOf(itemB), execute(adapter).items())    // Other item is returned
    }

    @Test
    fun `skip exception during leaf scrape doesn't kill everything`() {
      val adapter = adapter(listOf(
        More(url = ROOT_URL) {
          listOf(
            Leaf(name = "A", url = URL_A) { throw SkipItemException("Don't care") },
            Leaf(name = "A", url = URL_B) { itemB }
          )
        }
      ))

      assertEquals(listOf(itemB), execute(adapter).items())    // Other item is returned
    }
  }

  @Nested
  inner class Stats {
    @Test
    fun `counts normal`() {
      val adapter = adapterWithSingleLeaf { itemA }

      assertEquals(1, execute(adapter).stats.numRawItems)
    }

    @Test
    fun `counts malformed`() {
      val adapter = adapterWithSingleLeaf { throw MalformedInputException("Don't care") }

      assertEquals(1, execute(adapter).stats.numMalformed)
    }

    @Test
    fun `counts errors`() {
      val adapter = adapterWithSingleLeaf { throw RuntimeException("Don't care") }

      assertEquals(1, execute(adapter).stats.numErrors)
    }

    @Test
    fun `counts skipped`() {
      val adapter = adapterWithSingleLeaf { throw SkipItemException("Don't care") }

      assertEquals(1, execute(adapter).stats.numSkipped)
    }
  }

  private fun StatsWith<Result>.items() = entries.map { it.item }

  private fun execute(adapter: ScraperAdapter) = runBlocking { adapter.execute() }

  private fun docWithHeaderMatching(header: String): Document = argForWhich { textFrom("h1") == header }

  private fun adapterWithSingleLeaf(work: (Document) -> ScrapedItem) = adapter(listOf(
    More(url = ROOT_URL) {
      listOf(
        Leaf(name = "A", url = URL_A, work = work)
      )
    }
  ))

  private fun adapter(jobs: List<Job>) = ScraperAdapter(
    retriever,
    object : Scraper {
      override val jobs = jobs
    },
    BREWERY_NAME
  )

  companion object {
    private const val BREWERY_NAME = "Foo"
    private val ROOT_URL = URI("https://example.invalid")
    private val PAGE_2_URL = URI("https://example.invalid/2")
    private val URL_A = URI("https://example.invalid/a")
    private val URL_B = URI("https://example.invalid/a")
  }
}
