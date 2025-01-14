package de.westnordost.streetcomplete.osm

import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.mapdata.Element

/** Return whether this element is a kind of place, regardless whether it is currently vacant or
 *  not */
fun Element.isPlaceOrDisusedShop(): Boolean =
    isPlace() || isDisusedPlace()

/** Return whether this element is a kind of disused or vacant place */
fun Element.isDisusedPlace(): Boolean =
    IS_VACANT_PLACE_EXPRESSION.matches(this) ||
    this.asIfItWasnt("disused")?.let { IS_PLACE_EXPRESSION.matches(it) } == true

fun Element.isPlace(): Boolean =
    IS_PLACE_EXPRESSION.matches(this)

/** Map features like shops or amenities that usually have a name and can be entered.
 *
 *  To demarcate this category from others, the following are not included:
 *  - things that are rather structures or buildings than places (e.g. communication towers, train station)
 *  - landuses, outside-things (golf courses, zoos, marketplaces, car parking)
 *
 *  Note: When this function is modified, please follow update instructions in:
 *        https://github.com/mnalis/StreetComplete-taginfo-categorize/blob/master/README.md
 *
 *  Important: The contribution policy here is that only things may be added if an iD preset for it
 *             exists. This policy exists in order to reduce effort to maintain this list, i.e. we
 *             don't want to check, weigh and balance requests in parallel to iD maintainers (in
 *             terms of notability, it being unambiguous, consensus etc.)
 *  */
private val IS_PLACE_EXPRESSION by lazy {
    val tags = mapOf(
        "amenity" to listOf(
            /* grouped by subcategory, sorted by alphabet */

            /* sustenance */
            "bar",
            "biergarten",
            "cafe",
            "fast_food",
            "food_court",
            "ice_cream",
            "pub",
            "restaurant",

            /* education */
            "childcare",
            "college",
            "dancing_school",
            "dive_centre",
            "dojo",
            "driving_school",
            "kindergarten",
            "language_school",
            "library",
            "music_school",
            "prep_school",
            "research_institute",
            "school",
            "toy_library",
            "training",
            "university",

            /* transportation */
            "boat_rental", // usually there is some kind of office, even if it is just a small stall
            "car_rental", // usually there is some kind of office
            "car_wash",
            "fuel",
            "motorcycle_rental",
            "vehicle_inspection",

            /* financial */
            "bank",
            "bureau_de_change",
            "mobile_money_agent",
            "money_transfer",
            "payment_centre",

            /* healthcare */
            "clinic",
            "dentist",
            "doctors",
            "health_post",
            "hospital",
            "nursing_home",
            "pharmacy",
            "social_facility",
            "veterinary",

            /* entertainment, arts & culture */
            "archive",
            "arts_centre",
            "brothel",
            "casino",
            "cinema",
            "community_centre",
            "conference_centre",
            "events_venue",
            "exhibition_centre",
            "gambling",
            "hookah_lounge",
            "love_hotel",
            "music_venue",
            "nightclub",
            "planetarium",
            "ski_rental",
            "social_centre",
            "stripclub",
            "studio",
            "swingerclub",
            "theatre",

            /* public service */
            "courthouse",
            "embassy", // deprecated now
            "fire_station",
            "mailroom",
            "police",
            "post_depot",
            "post_office",
            "prison",
            "ranger_station",
            "townhall",

            /* facilities */
            "lavoir",
            "left_luggage",

            /* others */
            "animal_boarding",
            "animal_shelter",
            "animal_training",
            "coworking_space", // basically an office
            "crematorium",
            "funeral_hall",
            "internet_cafe",
            "meditation_centre",
            "monastery",
            "mortuary",
            "place_of_mourning",
            "place_of_worship",
            "public_bath",
        ),
        "emergency" to listOf(
            "air_rescue_service",
            "ambulance_station",
            "disaster_response",
            "mountain_rescue",
            "water_rescue",
        ),
        "leisure" to listOf(
            "adult_gaming_centre",
            "amusement_arcade",
            "bowling_alley",
            "dance",
            "escape_game",
            "fitness_centre",
            "hackerspace",
            "ice_rink",
            "indoor_play",
            "sauna",
            "sports_hall",
            "stadium",
            "tanning_salon",
            "trampoline_park",
        ),
        "military" to listOf(
            "office",
        ),
        "tourism" to listOf(
            "alpine_hut",
            "apartment",
            "aquarium",
            "chalet",
            "gallery",
            "guest_house",
            "hostel",
            "hotel",
            "hunting_lodge",
            // "information", only if it is an office, see below
            "museum",
            "motel",
            "trail_riding_station", // motel for riders
            "wilderness_hut"
        ),
    )
    .map { it.key + " ~ " + it.value.joinToString("|") }
    .joinToString("\n    or ")

    """
    nodes, ways, relations with
    $tags
    or club
    or craft
    or healthcare
    or office and office !~ no|vacant
    or shop and shop !~ no|vacant
    or tourism = information and information ~ office|visitor_centre
    """.toElementFilterExpression()
}

/** Expression to see if an element is some kind of vacant shop */
private val IS_VACANT_PLACE_EXPRESSION = """
    nodes, ways, relations with
      shop = vacant
      or office = vacant
      or amenity = vacant
""".toElementFilterExpression()

/** iD preset ids of popular place types */
val POPULAR_PLACE_FEATURE_IDS = listOf(
    // ordered roughly by usage number according to taginfo
    "amenity/restaurant", // 1.4 M
    "amenity/cafe", // 0.5 M
    "amenity/fast_food", // 0.5 M
    "shop/convenience", // 0.7 M
    "shop/supermarket", // 0.5 M
    "shop/clothes", // 0.4 M
    "shop/hairdresser", // 0.3 M
    "tourism/hotel", // 0.4 M
    "amenity/pharmacy", // 0.3 M
)

/** Replace a place with the given new tags.
 *  Removes any place-related tags before adding the given [tags]. */
fun StringMapChangesBuilder.replacePlace(tags: Map<String, String>) {
    removeCheckDates()

    for (key in keys) {
        if (KEYS_THAT_SHOULD_BE_REMOVED_WHEN_PLACE_IS_REPLACED.any { it.matches(key) }) {
            remove(key)
        }
    }

    for ((key, value) in tags) {
        this[key] = value
    }
}

// generated by "make update" from https://github.com/mnalis/StreetComplete-taginfo-categorize/
private val KEYS_THAT_SHOULD_BE_REMOVED_WHEN_PLACE_IS_REPLACED = listOf(
    "shop_?[1-9]?(:.*)?", "craft_?[1-9]?", "amenity_?[1-9]?", "old_amenity", "old_shop",
    "information", "leisure", "office_?[1-9]?", "tourism",
    // popular shop=* / craft=* subkeys
    "marketplace", "household", "swimming_pool", "laundry", "golf", "sports", "ice_cream",
    "scooter", "music", "retail", "yes", "ticket", "newsagent", "lighting", "truck", "car_repair",
    "car_parts", "video", "fuel", "farm", "car", "tractor", "hgv", "ski", "sculptor",
    "hearing_aids", "surf", "photo", "boat", "gas", "kitchen", "anime", "builder", "hairdresser",
    "security", "bakery", "bakehouse", "fishing", "doors", "kiosk", "market", "bathroom", "lamps",
    "vacant", "insurance(:.*)?", "caravan", "gift", "bicycle", "bicycle_rental", "insulation",
    "communication", "mall", "model", "empty", "wood", "hunting", "motorcycle", "trailer",
    "camera", "water", "fireplace", "outdoor", "blacksmith",
    // obsoleted information
    "abandoned(:.*)?", "disused(:.*)?", "was:.*", "not:.*", "damage", "source:damage",
    "created_by", "check_date", "opening_date", "last_checked", "checked_exists:date",
    "pharmacy_survey", "old_ref", "update", "import_uuid",
    // classifications / links to external databases
    "fhrs:.*", "old_fhrs:.*", "fvst:.*", "ncat", "nat_ref", "gnis:.*", "winkelnummer",
    "type:FR:FINESS", "type:FR:APE", "kvl_hro:amenity", "ref:DK:cvr(:.*)?", "certifications?",
    "transiscope", "opendata:type",
    // names and identifications
    "name_?[1-9]?(:.*)?", ".*_name_?[1-9]?(:.*)?", "noname", "branch(:.*)?", "brand(:.*)?",
    "not:brand(:.*)?", "network", "operator(:.*)?", "operator_type", "ref", "ref:vatin",
    "designation", "SEP:CLAVEESC", "identifier",
    // contacts
    "contact_person", "contact(:.*)?", "phone(:.*)?", "phone_?[1-9]?", "emergency:phone", "mobile",
    "fax", "facebook", "instagram", "twitter", "youtube", "telegram", "email",
    "website_?[1-9]?(:.*)?", "app:.*", "ownership",
    "url", "source_ref:url", "owner",
    // payments
    "payment(:.*)?", "payment_multi_fee", "currency:.*", "cash_withdrawal(:.*)?", "fee", "charge",
    "charge_fee", "money_transfer", "donation:compensation",
    // generic shop/craft attributes
    "seasonal", "time", "opening_hours(:.*)?", "check_date:opening_hours", "wifi", "internet",
    "internet_access(:.*)?", "second_hand", "self_service", "automated", "license:.*",
    "bulk_purchase", ".*:covid19", "language:.*", "baby_feeding", "description(:.*)?",
    "description[0-9]", "min_age", "max_age", "supermarket(:.*)?", "social_facility(:.*)?",
    "functional", "trade", "wholesale", "sale", "smoking", "zero_waste", "origin", "attraction",
    "strapline", "dog", "showroom", "toilets?(:.*)?", "changing_table", "wheelchair(.*)?", "blind",
    "company(:.*)?", "stroller", "walk-in", "webshop", "operational_status.*", "drive_through",
    "surveillance(:.*)?", "outdoor_seating", "indoor_seating", "colour", "access_simple", "floor",
    "product_category", "source_url", "category", "kids_area", "resort", "since", "state",
    "operational_status", "temporary",
    // food and drink details
    "bar", "cafe", "coffee", "microroasting", "microbrewery", "brewery", "real_ale", "taproom",
    "training", "distillery", "drink(:.*)?", "cocktails", "alcohol", "wine([:_].*)?",
    "happy_hours", "diet:.*", "cuisine", "tasting", "breakfast", "lunch", "organic",
    "produced_on_site", "restaurant", "food", "pastry", "pastry_shop", "product", "produce",
    "chocolate", "fair_trade", "butcher", "reservation(:.*)?", "takeaway(:.*)?", "delivery(:.*)?",
    "caterer", "real_fire", "flour_fortified", "highchair", "sport_pub",
    // related to repair shops/crafts
    "service(:.*)?", "motorcycle:.*", "repair", ".*:repair", "electronics_repair(:.*)?",
    "workshop",
    // shop=hairdresser, shop=clothes
    "unisex", "male", "female", "gender", "lgbtq(:.*)?",
    // healthcare
    "healthcare(:.*)?", "health", "health_.*", "medical_.*", "facility(:.*)?", "activities",
    "healthcare_facility(:.*)?", "laboratory(:.*)?", "blood(:.*)?", "blood_components",
    "infection(:.*)?", "disease(:.*)?", "covid19(:.*)?", "CovidVaccineCenterId",
    "coronaquarantine", "hospital(:.*)?", "hospital_type_id", "emergency_room",
    "sample_collection(:.*)?", "bed_count", "capacity:beds", "part_time_beds", "personnel:count",
    "staff_count(:.*)?", "admin_staff", "doctors_num", "nurses_num", "counselling_type",
    "testing_centres", "toilets_number", "urgent_care", "vaccination", "clinic", "hospital",
    "pharmacy", "laboratory", "sample_collection", "provided_for(:.*)?", "social_facility_for",
    "ambulance", "ward", "HSE_(code|hgid|hgroup|region)", "collection_centre", "design",
    "AUTORIZATIE", "reg_id", "scope", "ESTADO", "NIVSOCIO", "NO", "EMP_EST", "COD_HAB", "CLA_PERS",
    "CLA_PRES", "snis_code:.*", "hfac_bed", "hfac_type", "nature", "moph_code", "IJSN:.*",
    "massgis:id", "OGD-Stmk:.*", "paho:.*", "panchayath", "pbf_contract", "pcode", "pe:minsa:.*",
    "who:.*",
    // accommodation & layout
    "rooms", "stars", "accommodation", "beds", "capacity(:persons)?", "laundry_service",
    // misc specific attributes
    "clothes", "shoes", "tailor", "beauty", "tobacco", "carpenter", "furniture", "lottery",
    "sport", "leisure", "dispensing", "tailor:.*", "gambling", "material", "raw_material",
    "stonemason", "studio", "scuba_diving(:.*)?", "polling_station", "club", "collector", "books",
    "agrarian", "musical_instrument", "massage", "parts", "post_office(:.*)?", "religion",
    "denomination", "rental", ".*:rental", "tickets:.*", "public_transport", "goods_supply", "pet",
    "appliance", "artwork_type", "charity", "company", "crop", "dry_cleaning", "factory",
    "feature", "air_conditioning", "atm", "vending", "vending_machine", "recycling_type", "museum",
    "license_classes", "dance:style", "isced:level", "school", "preschool", "university",
    "research_institution", "research", "member_of", "topic", "townhall:type", "parish", "police",
    "government", "office", "administration", "administrative", "association", "transport",
    "utility", "consulting", "commercial", "private", "taxi", "admin_level", "official_status",
    "target", "liaison", "diplomatic(:.*)?", "embassy", "consulate", "aeroway", "department",
    "faculty", "aerospace:product", "boundary", "population", "diocese", "depot", "cargo",
    "function", "game", "party", "telecom(munication)?", "service_times", "kitchen:facilities",
    "it:(type|sales)", "cannabis:cbd",
    "camp_site", "camping", "emergency(:.*)?", "evacuation_cent(er|re)", "education",
    "engineering", "forestry", "foundation", "lawyer", "logistics", "military", "community_centre",
    "bank",
).map { it.toRegex() }
