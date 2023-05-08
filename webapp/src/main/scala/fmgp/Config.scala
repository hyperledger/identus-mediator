package fmgp

object Config {
  object PushNotifications {
    def publicKey = "BK0xP-d_SOEhucOAhadwSN-jILAqmkY2wGb5Ae56G8jFSivvgJV3Vw6AzVYmYi5M2grK_DC-0EZxsXIQBajW67s"

    val applicationServerKey = publicKey

    // yarn global add web-push
    // web-push send-notification --endpoint="https://fcm.googleapis.com/fcm/send/eugpAoA1AuY:APA91bG_bciEeMDKMjBJhrtNitzuEVbHbLvbs3gvacO1buLc6K53TCKYL4nS7ppEYRBZUS8uoRGiDWmn6biNAXFg_pN4s9udeOxZaQPyc8r2kD9S9hdjeUEOy6-VWDnx7XZpbleGrmn8" --key="BPQ3l9H6p7WnwEIToRI1PLMUuXw2Re18H7OWHMGsIGSOJ0gJi4vJMNdz5lrmWros6PJnXHXZtXI9HLmMOms6VaY" --auth="nGCRHNf6KLGrsidmYWJANQ" --payload="{}" --vapid-pubkey="BK0xP-d_SOEhucOAhadwSN-jILAqmkY2wGb5Ae56G8jFSivvgJV3Vw6AzVYmYi5M2grK_DC-0EZxsXIQBajW67s" --vapid-pvtkey="QzFqC67m9UrtBkUnL9H2ugLyeHqILjUCLsF_U2Yz8KM" --vapid-subject="https://did.fmgp.app"
  }

}
