package com.smarttour360.app.ui.common

data class DestinationSummary(
    val id: String,
    val name: String,
    val subtitle: String,
    val flag: String,
    val ecoScore: Int,
    val ethicalScore: String,
    val carbonKg: Int,
    val rating: Double,
    val imageUrl: String? = null,
    val imageAttribution: String? = null
)

data class HotelSummary(
    val id: String,
    val name: String,
    val subtitle: String,
    val priceText: String,
    val ecoScore: Int,
    val trend: String,
    val hasHiddenFees: Boolean,
    val ecoCertified: Boolean
)

data class BookingSummary(
    val hotelName: String,
    val stayInfo: String,
    val total: String,
    val flagInfo: String
)

object SampleData {
    val destinations = listOf(
        DestinationSummary("IND-SRN-01", "Srinagar", "Jammu and Kashmir", "GREEN", 84, "HIGH", 26, 4.8),
        DestinationSummary("IND-LEH-01", "Leh", "Ladakh", "YELLOW", 74, "HIGH", 41, 4.8),
        DestinationSummary("IND-AMR-01", "Amritsar", "Punjab", "GREEN", 80, "HIGH", 24, 4.7),
        DestinationSummary("IND-SML-01", "Shimla", "Himachal Pradesh", "GREEN", 79, "HIGH", 31, 4.7),
        DestinationSummary("IND-MAN-01", "Manali", "Himachal Pradesh", "GREEN", 78, "HIGH", 32, 4.7),
        DestinationSummary("IND-JPR-01", "Jaipur", "Rajasthan", "GREEN", 82, "HIGH", 27, 4.7),
        DestinationSummary("IND-UDR-01", "Udaipur", "Rajasthan", "GREEN", 83, "HIGH", 26, 4.8),
        DestinationSummary("IND-JSM-01", "Jaisalmer", "Rajasthan", "YELLOW", 70, "MODERATE", 38, 4.6),
        DestinationSummary("IND-RSH-01", "Rishikesh", "Uttarakhand", "GREEN", 81, "HIGH", 25, 4.7),
        DestinationSummary("IND-NAI-01", "Nainital", "Uttarakhand", "GREEN", 77, "HIGH", 29, 4.6),
        DestinationSummary("IND-VAR-01", "Varanasi", "Uttar Pradesh", "GREEN", 72, "HIGH", 28, 4.6),
        DestinationSummary("IND-AGR-01", "Agra", "Uttar Pradesh", "YELLOW", 69, "MODERATE", 36, 4.5),
        DestinationSummary("IND-GOA-01", "Goa", "Coastal Circuit", "YELLOW", 66, "MODERATE", 49, 4.5),
        DestinationSummary("IND-HMP-01", "Hampi", "Karnataka", "GREEN", 76, "HIGH", 30, 4.6),
        DestinationSummary("IND-MYS-01", "Mysuru", "Karnataka", "GREEN", 79, "HIGH", 26, 4.6),
        DestinationSummary("IND-KOC-01", "Kochi", "Kerala", "GREEN", 81, "HIGH", 24, 4.7),
        DestinationSummary("IND-MUN-01", "Munnar", "Kerala", "GREEN", 85, "HIGH", 22, 4.8),
        DestinationSummary("IND-ALP-01", "Alleppey", "Kerala", "GREEN", 80, "HIGH", 25, 4.7),
        DestinationSummary("IND-PON-01", "Pondicherry", "Tamil Nadu", "GREEN", 75, "HIGH", 29, 4.6),
        DestinationSummary("IND-MDU-01", "Madurai", "Tamil Nadu", "YELLOW", 68, "MODERATE", 35, 4.4),
        DestinationSummary("IND-HYD-01", "Hyderabad", "Telangana", "GREEN", 73, "HIGH", 30, 4.5),
        DestinationSummary("IND-KOL-01", "Kolkata", "West Bengal", "GREEN", 74, "HIGH", 31, 4.6),
        DestinationSummary("IND-DAR-01", "Darjeeling", "West Bengal", "GREEN", 82, "HIGH", 25, 4.7),
        DestinationSummary("IND-SIK-01", "Gangtok", "Sikkim", "GREEN", 83, "HIGH", 24, 4.8),
        DestinationSummary("IND-SHI-01", "Shillong", "Meghalaya", "GREEN", 81, "HIGH", 26, 4.7),
        DestinationSummary("IND-KAZ-01", "Kaziranga", "Assam", "GREEN", 78, "HIGH", 32, 4.6),
        DestinationSummary("IND-BHU-01", "Bhubaneswar", "Odisha", "GREEN", 71, "HIGH", 32, 4.4),
        DestinationSummary("IND-PUR-01", "Puri", "Odisha", "YELLOW", 67, "MODERATE", 37, 4.4)
    )

    val hotels = listOf(
        HotelSummary("HTL-001", "The Himalayan Resort", "Manali - Old Manali - Free cancellation", "Rs 3,500 / night", 80, "RISING", true, true),
        HotelSummary("HTL-002", "Snowcrest Eco Lodge", "Manali - Vashisht - Breakfast included", "Rs 4,200 / night", 84, "STABLE", false, true),
        HotelSummary("HTL-003", "Pine Valley Stay", "Shimla - Mall Road - Couple friendly", "Rs 2,900 / night", 68, "FALLING", false, false),
        HotelSummary("HTL-004", "Lake Palace Courtyard", "Udaipur - City Palace Road - Rooftop dinner", "Rs 5,200 / night", 82, "RISING", false, true),
        HotelSummary("HTL-005", "Pink City Haveli", "Jaipur - Bani Park - Breakfast included", "Rs 3,100 / night", 74, "STABLE", false, false),
        HotelSummary("HTL-006", "Backwater Bloom Suites", "Alleppey - Finishing Point - Canal view", "Rs 4,800 / night", 86, "RISING", false, true),
        HotelSummary("HTL-007", "Ghat View Residency", "Varanasi - Dashashwamedh - River access", "Rs 2,700 / night", 71, "STABLE", true, false),
        HotelSummary("HTL-008", "Tea Trail Bungalows", "Munnar - Tea Estate Road - Estate stay", "Rs 5,600 / night", 88, "RISING", false, true)
    )

    val cartItems = listOf(
        BookingSummary("The Himalayan Resort", "Apr 10-13 - 2 guests", "Rs 10,500", "RED flag at time of booking - Acknowledged"),
        BookingSummary("Shimla Heritage Inn", "Apr 14-15 - 2 guests", "Rs 4,200", "GREEN")
    )
}
