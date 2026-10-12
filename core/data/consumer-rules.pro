# Nabiralnik igrifikacije je shranjen kot JSON: imena polj morajo ostati enaka
-keepclassmembers class com.streak.app.data.prefs.InboxEventJson {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
