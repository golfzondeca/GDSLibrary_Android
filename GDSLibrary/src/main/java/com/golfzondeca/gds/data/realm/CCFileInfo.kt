package com.golfzondeca.gds.data.realm

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class CCFileInfo : RealmObject {
    @PrimaryKey
    var ccID = ""
    var countryCode = 0
    var stateCode = 0

    var downloadDate = RealmInstant.from(0, 0)

    var altitude = ""

    var holeMaps: RealmList<HoleMap> = realmListOf()
    var undulationMaps: RealmList<UndulationMap> = realmListOf()
}