package watch.craft.scrapers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import watch.craft.Offer
import watch.craft.Scraper.ScrapedItem
import watch.craft.byName
import watch.craft.executeScraper
import watch.craft.noDesc
import java.net.URI

@Disabled("Currently not selling anything")
class PollysScraperTest {
  companion object {
    private val ITEMS = executeScraper(PollysScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(5, ITEMS.size)
  }

  @Test
  fun `extracts beer details`() {
    assertEquals(
      ScrapedItem(
        name = "Mylar",
        summary = "IPA",
        abv = 6.3,
        offers = setOf(
          Offer(totalPrice = 4.50, sizeMl = 440)
        ),
        available = true,
        thumbnailUrl = URI("https://craftpeak-commerce-images.imgix.net/2020/07/MYL-01.png?auto=compress%2Cformat&fit=crop&h=324&ixlib=php-1.2.1&w=324&wpsize=woocommerce_thumbnail")
      ),
      ITEMS.byName("Mylar").noDesc()
    )
  }

  @Test
  fun `extracts description`() {
    assertNotNull(ITEMS.byName("Mylar").desc)
  }

  @Test
  fun `identifies sold out`() {
    assertFalse(ITEMS.byName("DDH Spur").available)
  }
}

