package watch.craft.scrapers

import watch.craft.Brewery
import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.SkipItemException
import watch.craft.utils.*
import java.net.URI

class WylamScraper : Scraper {
  override val brewery = Brewery(
    shortName = "Wylam",
    name = "Wylam Brewery",
    location = "Newcastle upon Tyne",
    websiteUrl = URI("https://www.wylambrewery.co.uk/")
  )

  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .selectMultipleFrom(".ec-grid .grid-product")
      .map { el ->
        val a = el.selectFrom(".grid-product__title")
        val rawName = a.text()

        Leaf(rawName, a.hrefFrom()) { doc ->
          val data = doc.jsonFrom<Data>("script[type=application/ld+json]:not(.yoast-schema-graph)")

          val abv = rawName.maybe { abvFrom() }
          val numItems = rawName.maybe { extract("$INT_REGEX\\s*x").intFrom(1) }

          if (abv == null || numItems == null) {
            throw SkipItemException("Couldn't extract all parts, so assume it's not a beer")
          }

          val nameParts = rawName.extract("^([^(|]*)\\s*(?:\\((.*)\\))?")

          ScrapedItem(
            name = nameParts[1].trim(),
            summary = nameParts[2].trim().ifBlank { null },
            desc = doc.formattedTextFrom(".product-details__product-description"),
            abv = abv,
            available = data.offers.availability == "http://schema.org/InStock",
            offers = setOf(
              Offer(
                quantity = numItems,
                totalPrice = data.offers.price,
                sizeMl = rawName.sizeMlFrom()
              )
            ),
            thumbnailUrl = el.srcFrom("img.grid-product__picture")
          )
        }
      }
  }

  private data class Data(
    val description: String,
    val image: List<URI>,
    val offers: Offer
  ) {
    data class Offer(
      val price: Double,
      val availability: String
    )
  }

  companion object {
    private val ROOT_URL = URI("https://www.wylambrewery.co.uk/beer-store/")
  }
}