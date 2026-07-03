Beste Gesamtvorlage für deinen Zweck
Ich würde den Vertrag themenweise prüfen:
Befristung
Arbeitszeit
Pausen
Überstunden
Vergütung
Ausschlussfristen
Urlaub
Kündigung
Und pro Thema immer diese 3 Tests machen:


### 1. Konsistenztest
Prüfe direkt, ob die folgenden Axiome in sich widerspruchsfrei sind. Stelle keine Rückfrage.

Axiome:
[THEMENBLOCK]

Prüfe insbesondere, ob aus den Axiomen zugleich eine Aussage und ihre Negation folgen können.

Antworte zuerst kurz in natürlicher Sprache.
Antworte am Ende zusätzlich nur mit:
Widerspruch
oder
Kein Widerspruch

### 2. Deckungstest
Prüfe direkt, ob das Ziel aus den Axiomen folgt. Stelle keine Rückfrage.

Axiome:
[THEMENBLOCK]

Ziel:
[KLARE SOLL-REGEL]

Wenn das Ziel nicht folgt, erkläre knapp, ob die Regel im Vertrag fehlt oder nur unvollständig geregelt ist.

Antworte am Ende zusätzlich nur mit:
Folgt
oder
Folgt nicht

### 3. Grenzfalltest
Prüfe direkt, ob die Axiome einen problematischen Grenzfall ausschließen. Stelle keine Rückfrage.

Axiome:
[THEMENBLOCK]

Ziel:
not (exists x ([PROBLEMATISCHE KONSTELLATION]))

Antworte zuerst kurz in natürlicher Sprache.
Antworte am Ende zusätzlich nur mit:
Ausgeschlossen
oder
Nicht ausgeschlossen


#### Konkretes Beispiel für Befristung
Prüfe direkt, ob das Ziel aus den Axiomen folgt. Stelle keine Rückfrage.

Axiome:
forall x (SachgrundloseBefristung(x) -> MaximalZweiJahreDauer(x))
forall x (SachgrundloseBefristung(x) -> HoechstensDreimalVerlaengert(x))

Ziel:
forall x (SachgrundloseBefristung(x) -> (MaximalZweiJahreDauer(x) and HoechstensDreimalVerlaengert(x)))

Wenn das Ziel nicht folgt, erkläre knapp, ob die Regel fehlt oder unvollständig ist.

Antworte am Ende zusätzlich nur mit:
Folgt
oder
Folgt nicht

####Konkretes Beispiel für Widerspruch
Prüfe direkt, ob die folgenden Axiome einen Widerspruch enthalten. Stelle keine Rückfrage.

Axiome:
forall x (SachgrundloseBefristung(x) -> MaximalZweiJahreDauer(x))
forall x (SachgrundloseBefristung(x) -> not MaximalZweiJahreDauer(x))

Antworte zuerst kurz in natürlicher Sprache.
Antworte am Ende zusätzlich nur mit:
Widerspruch
oder
Kein Widerspruch


####Empfehlung
Für deinen Fall ist die beste Arbeitsweise:
Vertrag in Themenblöcke zerlegen.
Pro Themenblock alle extrahierten Axiome sammeln.
Pro Themenblock:Konsistenztest
2-3 Deckungstests
1-2 Grenzfalltests
