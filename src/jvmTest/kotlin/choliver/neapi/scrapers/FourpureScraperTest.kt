package choliver.neapi.scrapers

import choliver.neapi.ParsedItem
import choliver.neapi.executeScraper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.text.RegexOption.IGNORE_CASE

class FourpureScraperTest {
  companion object {
    private val ITEMS = executeScraper(FourpureScraper())
  }

  @Test
  fun `finds all the beers`() {
    assertEquals(10, ITEMS.size)
  }

  @Test
  fun `excludes packs`() {
    assertTrue(ITEMS.none { it.name.contains("pack".toRegex(IGNORE_CASE)) })
  }

  @Test
  fun `strips size from non-keg name`() {
    assertTrue(ITEMS.none { it.name.contains("ml", ignoreCase = true) })
  }

  @Test
  fun `extracts non-keg details`() {
    assertEquals(
      ParsedItem(
        name = "Basecamp",
        perItemPrice = 2.00,
        abv = 4.7,
        sizeMl = 330,
        available = true,
        thumbnailUrl = URI("https://www.fourpure.com/uploads/images/products/thumbs/fourpurebrewingco._fourpure_pilslager_1566986321BASECAMPNB.png"),
        url = URI("https://www.fourpure.com/item/3/Fourpure/Basecamp.html")
      ),
      ITEMS.first { it.name == "Basecamp" }
    )
  }

  @Test
  fun `extracts keg details`() {
    assertEquals(
      ParsedItem(
        name = "Juicebox",
        summary = "Minikeg",
        perItemPrice = 35.00,
        abv = 5.9,
        sizeMl = 5000,
        available = true,
        thumbnailUrl = URI("https://www.fourpure.com/uploads/images/products/thumbs/fourpurebrewingco._fourpure_juicebox5lminikeg_1588779669WhatsAppImage20200506at14.07.452.jpeg"),
        url = URI("https://www.fourpure.com/item/286/Fourpure/Juicebox-5L-Minikeg.html")
      ),
      ITEMS.first { it.name == "Juicebox" && it.summary == "Minikeg" }
    )
  }
}

