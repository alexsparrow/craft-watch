package watch.craft.scrapers

import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.utils.*
import java.net.URI

class HackneyChurchScraper : Scraper {
  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .selectMultipleFrom("#Collection .hcbc-collection-grid-item")
      .map { el ->
        val rawName = el.textFrom(".grid-view-item__title")

        Leaf(rawName, el.hrefFrom("a.grid-view-item__link")) { doc ->
          val price = el.selectFrom(".price")
          val desc = doc.formattedTextFrom(".hcbc-product-description")
          val descLines = desc.split("\n")

          val allQuantities = descLines
            .mapNotNull { it.maybe { extract("(\\d+)\\s*x").intFrom(1) } }

          val distinctSizes = desc.split("\n")
            .mapNotNull { it.maybe { desc.sizeMlFrom() } }
            .distinct()

          ScrapedItem(
            name = rawName,
            summary = null,
            desc = desc,
            mixed = allQuantities.size > 1,
            abv = null,
            available = !price.text().contains("sold out", ignoreCase = true),
            offers = setOf(
              Offer(
                quantity = allQuantities.sum(),
                totalPrice = price.priceFrom(),
                sizeMl = if (distinctSizes.size == 1) distinctSizes.first() else null
              )
            ),
            thumbnailUrl = URI(
              // The URLs are dynamically created
              el.attrFrom("img.grid-view-item__image", "abs:data-src")
                .replace("{width}", "200")
            )
          )
        }
      }
  }

  companion object {
    val ROOT_URL = URI("https://hackneychurchbrew.co/collections/beers")
  }
}
