package watch.craft.scrapers

import watch.craft.Format
import watch.craft.Format.KEG
import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.shopify.shopifyItems
import watch.craft.utils.*
import java.net.URI

class PillarsScraper : Scraper {
  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .shopifyItems()
      .map { details ->
        Leaf(details.title, details.url) { doc ->
          val titleParts = extractTitleParts(details.title)
          val descParts = doc.orSkip("Couldn't find style or ABV") {
            extractFrom(".product-single__description", "STYLE:\\s+(.+?)\\s+ABV:\\s+(\\d\\.\\d+)%")
          }  // If we don't see these fields, assume we're not looking at a beer product

          val mixed = details.title.contains("mixed", ignoreCase = true)

          ScrapedItem(
            thumbnailUrl = details.thumbnailUrl,
            name = titleParts.name,
            summary = descParts[1].toTitleCase(),
            desc = doc.maybe { formattedTextFrom(".product-single__description") },
            mixed = mixed,
            abv = if (mixed) null else descParts[2].toDouble(),
            available = details.available,
            offers = setOf(
              Offer(
                quantity = titleParts.numItems,
                totalPrice = details.price,
                format = titleParts.format,
                sizeMl = titleParts.sizeMl
              )
            )
          )
        }
      }
  }

  private data class TitleParts(
    val name: String,
    val sizeMl: Int? = null,
    val numItems: Int = 1,
    val format: Format? = null
  )

  private fun extractTitleParts(title: String) = when {
    title.contains("Case") -> {
      val parts = title.extract("(.*?) Case of (\\d+)")
      TitleParts(name = parts[1], numItems = parts[2].toInt())
    }
    title.contains("Keg") -> {
      TitleParts(
        name = title.extract("(.*?) \\d+")[1],
        sizeMl = title.sizeMlFrom(),
        format = KEG
      )
    }
    else -> TitleParts(name = title)
  }

  companion object {
    private val ROOT_URL = URI("https://shop.pillarsbrewery.com/collections/pillars-beers")
  }
}
