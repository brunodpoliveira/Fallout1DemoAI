package utils

data class MetadataInfo(
    val hasSecret: Boolean,
    val hasConspiracy: Boolean,
    val secretParticipants: List<String>,
    val conspirators: List<String>
)

