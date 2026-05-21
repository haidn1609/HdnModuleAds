# Function:  
Support load and show ads  
# How to use  
## Import
Add it in your settings.gradle.kts at the end of repositories:
```kotlin
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```
Add the dependency
```kotlin
dependencies {
	        implementation("com.github.haidn1609:HdnModuleAds:1.0.0")
	}
```
## Init
In Application:  
```kotlin
 MobileAds.initialize(
                context,
                initializationStatus -> {
                });
```
## Usage
### Open
set id
```kotlin
//id is string id or string list id
AdsManager.setOpaIdRl(id) // id release
AdsManager.setOpaIdRm(id) // idRemote
```
init in application
```kotlin
new OpenAdsHelper().setup(application);
//disable activity if you dont want show
OpenAds.disableAdsOpenForActivity(YourActivity.class);
```
load ads
```kotlin
OpenAds.initOpenAds(activity){
        //action load done
}
```
### Interstitial
Set id
```kotlin
//id is string id or string list id
AdsManager.setItaIdRl(id) // id release
AdsManager.setItaIdRm(id) // idRemote
```
load ads
```kotlin
 InterAds.initInterAds(
            context = activity,
            onLoadError = {
                //action load err
            },
            onLoadSuccess = {
                //action load success
            })
```
show ads
```kotlin
AdsManager.showInterAds(activity){
          //action when show done
      }
```
### Interstitial Splash
Set id
```kotlin
//id is string id or string list id
AdsManager.setItsaIdRl(id) // id release
AdsManager.setItsaIdRm(id) // idRemote
```
load ads
```kotlin
InterSplashAds.initInterAds(activity){
            //action load done
        }
```
show ads
```kotlin
//show not have dialog after done
 showInterSplashAds(
           activity=activity,
           startCallback={
               //action when start show
           },
           doneCallBack={
               //action when show done
           }
       )
//show have dialog after done
 showInterSplashAds(
            activity = activity,
            dialogRes = dialogRes,
            nativeKey = nativeKey,
            startCallback = {
                //action when start show
            },
            doneCallBack = {
                //action when show done
            }
        )
```
### Banner Ads
Set id
```kotlin
//id is string id or string list id
AdsManager.setBnaIdRl(id) // id release
AdsManager.setBnaIdRm(id) // idRemote
```
show ads
```kotlin
// normal banner
AdsManager.showBannerAds(activity, BannerType.NORMAL, viewContainer)
// collapse banner
AdsManager.showBannerAds(activity, BannerType.COLLAPSIBLE, viewContainer)
```
### Reward Ads
Set id
```kotlin
//id is string id or string list id
AdsManager.setRwaIdRl(id) // id release
AdsManager.setRwaIdRm(id) // idRemote
```
show
```kotlin
AdsManager.showRewardAds(activity, object : RewardCallback {
            override fun onAdShowed() {
                //start show ads
            }

            override fun onAdClosed() {
                //ad close
            }

            override fun onRewardEarned() {
                //action earn reward
            }

            override fun onAdFailed() {
                // show ads failed
            }

            override fun onPremium() {
                //action earn reward when premium
            }
        })
```
### Native Ads
create native ads
```kotlin
//id is string id or string list id
AdsManager.addNative("native_dialog_full",
              encodeAdIds(id),//release id
              encodeAdIds(id);//remote id
```
use
```kotlin
//load
AdsManager.preloadNative(activity,key);
//show
AdsManager.showNative(activity,key,resId,viewContainer);
//load and show
AdsManager.loadAndShowNative(activity,key,resId,viewContainer);
```
### Other
```kotlin
//set use ad debug
AdsManager.setDebug(true);
//set ads enabled
AdsManager.setEnabled(true);
//set vip/premium
AdsManager.setVip(true);
//set callback adsPair
AdsManager.adsPair = {adValue->
            //action  pair
        }
//set callback log ads
 AdsManager.adsLog = {adLog->
            //action log
        }
```
