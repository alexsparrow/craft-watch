import _ from "underscore";
import React from "react";
import { Item, Offer, Format } from "../utils/model";
import SortableTable, { Column, Section, CellProps } from "./SortableTable";
import { headlineOffer } from "../utils/stuff";
import { OUT_OF_STOCK, MINIKEG, MIXED_CASE } from "../utils/strings";
import { splitToParagraphs } from "../utils/reactUtils";
import { BreweryLink } from "./BreweryLink";

interface Props {
  items: Array<Item>;
  categories: Array<string>;
}

const InventoryTable: React.FC<Props> = (props) => (
  <SortableTable sections={partitionItems(props.items, props.categories)}>
    <Column render={BreweryInfo} name="Brewery" className="brewery" selector={item => item.brewery} />
    <Column render={Thumbnail} className="thumbnail" />
    <Column render={NameInfo} name="Name" className="name" selector={item => item.name} />
    <Column render={AbvInfo} name="ABV" className="hide-tiny" selector={item => item.abv} />
    <Column render={PriceInfo} name="Price" className="price" selector={item => perItemPrice(headlineOffer(item))} />
  </SortableTable>
);

const BreweryInfo = ({ datum }: CellProps<Item>) => (
  <BreweryLink shortName={datum.brewery.shortName}>
    {datum.brewery.shortName}
  </BreweryLink>
);

const Thumbnail = ({ datum }: CellProps<Item>) => (
  <a href={datum.url}>
    <img alt="" src={datum.thumbnailUrl} width="100px" height="100px" />
    {datum.available || <div className="sold-out">{OUT_OF_STOCK}</div>}
  </a>
);

const NameInfo = ({ datum }: CellProps<Item>) => {
  const newItem = datum.new && !datum.brewery.new;
  const justAdded = datum.new && datum.brewery.new;
  const keg = headlineOffer(datum).format === Format.Keg;
  const kegAvailable = !keg && _.any(_.rest(datum.offers), offer => offer.format === Format.Keg);
  const mixed = datum.mixed;

  return (
    <div className="tooltip">
      <a className="item-link" href={datum.url}>{datum.name}</a>
      <p className="summary">
        {datum.summary}
      </p>
      <p className="summary">
        {newItem && <span className="pill new">NEW !!!</span>}
        {justAdded && <span className="pill just-added">Just added</span>}
        {keg && <span className="pill keg">{MINIKEG}</span>}
        {kegAvailable && <span className="pill keg">Minikeg available</span>}
        {mixed && <span className="pill mixed">{MIXED_CASE}</span>}
      </p>
      {(datum.desc !== undefined) && <TooltipBody datum={datum} />}
    </div>
  );
};

const AbvInfo = ({ datum }: CellProps<Item>) => (
  <>
    {(datum.abv !== undefined) ? `${datum.abv.toFixed(1)}%` : "?"}
  </>
);

const PriceInfo = ({ datum }: CellProps<Item>) => (
  <>
    <OfferInfo offer={headlineOffer(datum)} />
    {
      (_.size(datum.offers) > 1) && (
        <details>
          <summary>{_.size(datum.offers) - 1} more</summary>
          {
            _.map(_.rest(datum.offers), (offer, idx) => <OfferInfo key={idx} offer={offer} />)
          }
        </details>
      )
    }
  </>
);

const OfferInfo = ({ offer }: { offer: Offer }) => {
  const sizeString = sizeForDisplay(offer);
  const formatString = formatForDisplay(offer);
  return (
    <div className="offer">
      £{priceForDisplay(offer)} <span className="summary hide-small">/ {formatString}</span>
      <p className="summary">
        {
          (offer.quantity > 1) ? `${offer.quantity} × ${sizeString ?? `${formatString}s`}` : sizeString
        }
      </p>
    </div>
  );
};

// These are positioned all wrong on mobile, so disable when things get small
const TooltipBody = ({ datum }: CellProps<Item>) => (
  <span className="tooltip-text hide-small" style={{ display: "hidden" }}>
    {(datum.desc !== undefined) && splitToParagraphs(datum.desc)}
    <div className="disclaimer">© {datum.brewery.shortName}</div>
  </span>
);

// TODO - may want to randomise selection for items with more than one category
const partitionItems = (items: Array<Item>, categories: Array<string>): Array<Section<Item>> => {
  const other = "Other";
  const partitioned = _.groupBy(items, item => item.categories[0] || other);
  // Ensure we uphold preferred display order
  return _.map(categories.concat(other), c => ({ name: c, data: partitioned[c] }));
};

const sizeForDisplay = (offer: Offer): string | undefined =>
  (offer.sizeMl === undefined) ? undefined :
  (offer.sizeMl < 1000) ? `${offer.sizeMl} ml` :
  `${offer.sizeMl / 1000} litres`;

const formatForDisplay = (offer: Offer): string => {
  switch (offer.format) {
  case Format.Bottle:
    return "bottle";
  case Format.Can:
    return "can";
  case Format.Keg:
    return "keg";
  default:
    return "item";
  }
};

const priceForDisplay = (offer: Offer): string => perItemPrice(offer).toFixed(2).replace(/\.00/, "");

const perItemPrice = (offer: Offer): number => offer.totalPrice / offer.quantity;

export default InventoryTable;
