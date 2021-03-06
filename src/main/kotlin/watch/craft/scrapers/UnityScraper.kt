package watch.craft.scrapers

import watch.craft.Offer
import watch.craft.Scraper
import watch.craft.Scraper.Job.Leaf
import watch.craft.Scraper.ScrapedItem
import watch.craft.shopify.shopifyItems
import watch.craft.utils.abvFrom
import watch.craft.utils.forRootUrls
import watch.craft.utils.sizeMlFrom
import watch.craft.utils.textFrom
import java.net.URI

class UnityScraper : Scraper {
  override val jobs = forRootUrls(ROOT_URL) { root ->
    root
      .shopifyItems()
      .map { details ->

        Leaf(details.title, details.url) { doc ->
          val desc = doc.textFrom(".product-single__description")

          ScrapedItem(
            name = details.title,
            summary = null,
            desc = desc,
            mixed = false,
            abv = desc.abvFrom(),
            available = details.available,
            offers = setOf(
              Offer(
                totalPrice = details.price,
                sizeMl = desc.sizeMlFrom()
              )
            ),
            thumbnailUrl = details.thumbnailUrl
          )
        }
      }
  }

  companion object {
    private val ROOT_URL = URI("https://unitybrewingco.com/collections/unity-beer")
  }
}
