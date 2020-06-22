package choliver.neapi

import choliver.neapi.model.Inventory
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.browser.document
import kotlin.browser.window
import kotlin.text.Typography.nbsp

fun main() {
  window.fetch("/inventory.json")
    .then { it.text() }
    .then {
      val json = Json(JsonConfiguration.Stable)
      // TODO - error handling
      updateDom(json.parse(Inventory.serializer(), it))
    }
}

fun updateDom(inventory: Inventory) {
  document.body!!.append.div {
    table {
      thead {
        tr {
          th { +"Brewery" }
          th { +" " }
          th {
            classes += "name"
            +"Name"
          }
          th { +"ABV" }
          th { +"Size" }
          th { +"Price per item" }
        }
      }
      tbody {
        inventory.items.forEach { item ->
          tr {
            td { +item.brewery }
            td {
              classes += "thumbnail"
              if (item.thumbnailUrl != null) {
                a(item.url) {
                  img(src = item.thumbnailUrl) {
                    width = "100px"
                    height = "100px"
                  }
                }
                if (!item.available) {
                  div {
                    classes += "sold-out"
                    +"Sold out"
                  }
                }
              }
            }
            td {
              classes += "name"
              a(item.url) {
                +item.name
              }
              if (item.summary != null) {
                p {
                  classes += "summary"
                  +item.summary
                }
              }
            }
            td {
              if (item.abv != null) {
                +"${item.abv.asDynamic().toFixed(1)}%"
              } else {
                +"?"
              }
            }
            td {
              +when (item.sizeMl) {
                null -> "?"
                in (0 until 1000) -> "${item.sizeMl}${nbsp}ml"
                else -> "${item.sizeMl / 1000}${nbsp}litres"
              }
            }
            td { +"£${item.perItemPrice.asDynamic().toFixed(2)}" }
          }
        }
      }
    }
  }
}