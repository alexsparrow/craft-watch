package choliver.neapi.scrapers

import choliver.neapi.ParsedItem
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

class GipsyHillScraperTest {
  private val raw = {}.javaClass.getResource("/samples/gipsy-hill.html").readText()
  private val doc = Jsoup.parse(raw)
  private val items = GipsyHillScraper().scrape(doc)

  @Test
  fun `finds all the beers`() {
    assertEquals(18, items.size)
  }

  @Test
  fun `eliminates duplicates`() {
    assertEquals(1, items.filter { it.name == "Moneybags" }.size)
  }

  @Test
  fun `extracts beer details`() {
    assertTrue(
      ParsedItem(
        name = "Carver",
        price = "2.20".toBigDecimal(),
        available = true,
        thumbnailUrl = URI("https://i1.wp.com/gipsyhillbrew.com/wp-content/uploads/2018/11/CARVER.png?resize=300%2C300&ssl=1"),
        url = URI("https://gipsyhillbrew.com/product/carver/")
      ) in items
    )
  }
}

