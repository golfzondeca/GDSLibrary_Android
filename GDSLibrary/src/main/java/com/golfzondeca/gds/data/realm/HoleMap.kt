package com.golfzondeca.gds.data.realm

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class HoleMap: RealmObject {
    @PrimaryKey
    var courseNum = 1
    var file = ""
}