package com.golfzondeca.gds.volley

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

/**
 * 골프버디 웹서버 CC 검색 정보 XML Response Model
 *
 * @author lookjoon
 */

@Root(name = "document", strict = false)
class CCSearchResponse {
    @field:Element(name = "result", required = true)
    var resultCode: Int? = 0

    @field:Element(name = "searchcc", required = false)
    lateinit var searchCC: SearchCCElement
}

@Root(name = "searchcc", strict = false)
class SearchCCElement {
    @field:ElementList(entry = "cc", inline = true)
    lateinit var ccList: List<CCElement>
}

@Root(name = "cc", strict = false)
class CCElement {
    @field:Element(name = "ccid", required = false)
    var ccid: String? = ""

    @field:Element(name = "ccname", required = false)
    var name: String? = ""

    @field:Element(name = "ccaddr", required = false)
    var address: String? = ""

    @field:Element(name = "distance", required = false)
    var distance: String? = ""

    @field:Element(name = "countrycode", required = false)
    var countryCode: String? = ""

    @field:Element(name = "statecode", required = false)
    var stateCode: String? = ""

    @field:Element(name = "cscount", required = false)
    var csCount: String? = ""

    @field:Element(name = "mile", required = false)
    var mile: String? = ""

    @field:Element(name = "date", required = false)
    var date: String? = ""

    @field:Element(name = "updatedate", required = false)
    var updateDate: String? = ""

    @field:Element(name = "updateyn", required = false)
    var updateYN: String? = ""

    var isFavoriteCC: Boolean = false
}