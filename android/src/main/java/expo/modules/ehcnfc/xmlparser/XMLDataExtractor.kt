package expo.modules.ehcnfc.xmlparser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.io.IOException

sealed class XMLParsingError(message: String) : Exception(message) {
    object DataConversionFailed : XMLParsingError("XML data conversion failed")
    object ParsingFailed : XMLParsingError("XML parsing failed")
    class UnknownElement(val elementName: String) : XMLParsingError("Unknown XML element: $elementName")
}

class XMLDataExtractor {

    private val elementsToExtract = setOf(
        "Versicherten_ID", "Geburtsdatum", "Vorname", "Nachname", "Namenszusatz",
        "Geschlecht", "Postleitzahl", "Ort", "Strasse",
        "Hausnummer", "Wohnsitzlaendercode", "Titel"
    )

    @Throws(XMLParsingError::class)
    fun parse(xmlString: String): MutableMap<String, String> {
        val parsedData = mutableMapOf<String, String>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))

        var eventType = parser.eventType
        var currentElement: String? = null
        val foundCharacters = StringBuilder()

        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentElement = parser.name
                        foundCharacters.clear()
                        if (currentElement !in elementsToExtract) {
                            println(XMLParsingError.UnknownElement(currentElement).message)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (currentElement in elementsToExtract) {
                            foundCharacters.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val elementName = parser.name
                        if (elementName in elementsToExtract) {
                            parsedData[elementName] = foundCharacters.toString().trim()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            throw XMLParsingError.ParsingFailed
        } catch (e: IOException) {
            throw XMLParsingError.DataConversionFailed
        }

        return parsedData
    }
}
