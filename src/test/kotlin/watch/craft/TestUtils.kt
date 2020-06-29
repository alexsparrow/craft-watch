package watch.craft

import mu.KotlinLogging
import watch.craft.getters.CachingGetter
import watch.craft.getters.HtmlGetter
import watch.craft.getters.HttpGetter
import watch.craft.storage.*
import java.time.LocalDate
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

private val GOLDEN_TIMESTAMP = LocalDate.parse("2020-06-28")
  .atStartOfDay(ZoneOffset.UTC).toInstant()

fun executeScraper(scraper: Scraper): List<Scraper.Item> {
  // TODO - make this configurable
  val store = ReadOnlyObjectStore(
    WriteThroughObjectStore(
      firstLevel = LocalObjectStore(CACHE_DIR),
      secondLevel = GcsObjectStore(GCS_BUCKET)
    )
  )
  val structure = StoreStructure(store, GOLDEN_TIMESTAMP)
  val cachingGetter = CachingGetter(structure.cache, HttpGetter())
  val getter = HtmlGetter(cachingGetter)
  return scraper.scrapeIndex(getter.request(scraper.rootUrl))
    .mapNotNull {
      try {
        it.scrapeItem(getter.request(it.url))
      } catch (e: SkipItemException) {
        logger.info("Skipped because: ${e.message}")
        null
      } catch (e: NonFatalScraperException) {
        logger.warn("Non-fatal exception: ${e.message}")
        null
      }
    }
}

fun List<Scraper.Item>.byName(name: String) = first { it.name == name }

fun Scraper.Item.noDesc() = copy(desc = null)    // Makes it easier to test item equality