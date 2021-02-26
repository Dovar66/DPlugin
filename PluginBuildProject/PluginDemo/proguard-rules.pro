# ---------------------------------------------
# **不要改动**
## android sdk base#########################################
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider


-ignorewarning

#指定不混淆给定的包名称。*package_filter*过滤器是以逗号分隔的包名称列表。包名称可以包含 **?**、** * **、** ** ** 通配符。
-keeppackagenames 'com.dovar.plugin'

# ---------------------------------------------

-keep class com.dovar.plugin.TestDynamicImpl {*;}