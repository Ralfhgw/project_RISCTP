# Idee des Projektes

Dieses Projekt sollte bewusst keinen besonders mächtigen agentischen KI-Assistenten bauen, der mit großen Spezialsystemen konkurriert. Das eigentliche Ziel war viel grundlegender: Es sollte untersucht werden, wie man ein Large Language Model mit möglichst wenig Aufwand an ein externes logisches Werkzeug koppeln kann.

Dafür verwendet das Projekt das ReAct-Prinzip. Das bedeutet: Das Sprachmodell antwortet nicht nur mit Text, sondern kann auch ein externes Werkzeug anfordern. Dessen Ausgabe wird dann wieder in den weiteren Verlauf eingebunden. In diesem Projekt wurde dafür absichtlich der bereits vorhandene ReAct-Workflow von LangChain genutzt und kein aufwendigeres Verfahren mit komplexen Denk- oder Ausführungsbäumen.

### Start Applikation
 ```
 $ cd agent/
 $ python3 -m venv venv
 $ source venv/bin/activate
 $ pip install flask flask-cors langchain langchain-openai langchain-google-genai langchain-anthropic langchain-ollama langgraph requests
 $ python3 server.py 
```

### Projektübersicht

Dieses Projekt verbindet zwei Teile:

- `RISCTP`: das eigentliche Beweiswerkzeug für logische Probleme
- `agent`: eine kleine Chat-Anwendung, die natürlichsprachige Anfragen entgegennimmt und sie an das Beweiswerkzeug weitergibt

Die Grundidee ist einfach: Ein Nutzer beschreibt Axiome und ein Ziel in natürlicher Sprache oder in formaler Logik. Der Agent bereitet diese Eingabe auf, prüft die Struktur und übergibt das Problem dann an RISCTP, damit ein Beweis oder eine Widerlegung versucht werden kann.

### Wie das Gesamtprojekt arbeitet

Der Ablauf ist grob so:

1. Die Weboberfläche oder ein API-Aufruf schickt eine Nachricht an den Server.
2. Der Agent übersetzt die Anfrage in ein internes Logikformat.
3. Parser und Checker prüfen, ob die Eingabe syntaktisch und inhaltlich gültig ist.
4. Das Problem wird in die RISCTP-Syntax umgewandelt.
5. RISCTP und der angebundene Solver versuchen den Beweis.
6. Das Ergebnis wird an den Agenten und dann an die Oberfläche zurückgegeben.

Damit ist das Projekt eine Brücke zwischen menschlicher Eingabe und einem formalen Beweiswerkzeug.

### Wie die externen Tools angesprochen werden

Der wichtigste Mechanismus ist in `agent.py` zu sehen. Dort werden Werkzeuge als Tools definiert und anschließend dem Agenten übergeben:

```python
@tool
def download(url: str) -> str:
    """Download resource from the denoted URL and return its content."""
    ...

@tool
def prove(fol_text: str, interactive: bool=False) -> str:
    success, output = risctp_prover.prove(fol_text, interactive)
    if success:
        return f"BEWEIS ERFOLGREICH (VALID).\nHier ist die Ausgabe des Solvers:\n{output}"
    return f"STOPP: Der Beweis konnte nicht erfolgreich abgeschlossen werden.\nHier ist das Protokoll:\n{output}"

class MultiModelAgent:
    def __init__(self, model_type: str):
        self.tools = [download, prove]
        self.agent_executor = create_agent(
            self.llm,
            self.tools,
            checkpointer=self.memory
        )
```

Genau an dieser Stelle wird das Sprachmodell mit externen Funktionen verbunden. Der Agent kann dadurch nicht nur Text erzeugen, sondern bei Bedarf gezielt das Beweiswerkzeug aufrufen.

#### Fall 1: Das `download`-Tool

Dieses Tool ist dafür da, eine externe Ressource oder eine Hilfedatei zu laden, zum Beispiel eine Beschreibung des FOL-PRE-Formats.

Ablauf:

`Chat-Eingabe`
Der Nutzer fragt zum Beispiel nach dem Logikformat oder der Bedeutung einer Datei.

`LLM analysiert die Anfrage`
Das Modell erkennt, dass es dafuer zusätzliche Informationen aus einer Datei oder URL braucht.

`LLM ruft download(...) auf`
Im Code passiert das über das als Tool registrierte `download`-Werkzeug.

`download(...) liefert Text zurueck`
Der Inhalt wird in den weiteren Agentenverlauf eingespeist.

`LLM nutzt den geladenen Text fuer die Antwort`
Erst danach formuliert das Modell die sichtbare Antwort im Chat.

#### Fall 2: Das `prove`-Tool

Dieses Tool ist der eigentliche Weg vom Chat zum Beweiswerkzeug RISCTP.

Ablauf:

`Chat-Eingabe`
Der Nutzer gibt Axiome und ein Ziel ein.

`LLM analysiert die Anfrage`
Das Modell erkennt, dass ein Beweis oder Gegenbeweis versucht werden soll.

`LLM baut daraus FOL-PRE-Code`
Die Anfrage wird in das interne formale Format gebracht.

`LLM ruft prove(fol_text, interactive=False) auf`
Der fertige formale Text wird an das Tool übergeben.

`prove(...) ruft den Python-Prover-Adapter auf`
Dort wird der Text geparst, geprüft und in RISCTP-Syntax umgewandelt.

`RISCTP / Solver wird gestartet`
Jetzt läuft der eigentliche technische Beweisschritt ausserhalb des LLM.

`Ergebnis kommt an das Tool zurueck`
Das Tool gibt Erfolg oder Fehlertext an den Agenten zurück.

`LLM formuliert daraus die sichtbare Chat-Antwort`
Der Nutzer sieht am Ende eine sprachliche Antwort, nicht direkt den kompletten internen Ablauf.

## Dateien im Ordner `agent`

`agent.py` - Die zentrale Steuerdatei des Chat-Agenten. Hier werden die Sprachmodelle eingebunden, die Tools definiert und der Gesprächsablauf gesteuert. Außerdem wird hier die Logikeingabe vor dem Beweis noch bereinigt und normalisiert.

`server.py` - Ein kleiner Flask-Server für die Webanwendung. Er stellt die HTTP-Schnittstellen bereit, nimmt Nachrichten entgegen, ruft den Agenten auf und liefert die Antwort als JSON zurück.

`prover.py` - Die Verbindung zwischen Agent und RISCTP. Diese Datei prüft ein formales Problem, wandelt es in die RISCTP-Darstellung um, startet den Prover und gibt das Ergebnis zurück.

`parser.py` - Der Parser für das FOL-PRE-Format. Er liest die formale Eingabe und baut daraus eine interne Baumstruktur auf, mit der das Projekt weiterarbeiten kann.

`scanner.py` - Der Scanner zerlegt den Eingabetext zuerst in kleine Bestandteile, also Tokens. Diese Vorarbeit braucht der Parser, um die Struktur der Formeln zu erkennen.

`checker.py` - Diese Datei überprüft, ob ein formales Problem inhaltlich stimmig ist. Zum Beispiel wird kontrolliert, ob verwendete Typen, Prädikate, Funktionen und Variablen korrekt deklariert wurden.

`risctp.py` - Hier wird die interne Darstellung eines Problems in die konkrete RISCTP-Syntax umgewandelt. Die Datei ist also der Übersetzer zwischen der Python-internen Struktur und dem externen Beweiswerkzeug.

`executor.py` - Hilfsfunktionen zum Starten und Stoppen externer Programme. Diese Datei kümmert sich darum, RISCTP oder andere Prozesse aus Python heraus auszuführen und deren Ausgabe einzusammeln.

`exp.py` - Definiert die grundlegende Datenstruktur für logische Ausdrücke. Diese Struktur wird von Parser, Checker und Üersetzer gemeinsam verwendet.

`index.html` - Die einfache Weboberfläche des Projekts. Hier kann der Nutzer Nachrichten eingeben und die Antworten des Agenten im Browser sehen.

`FOL-PRE.txt` - Eine Beschreibung des verwendeten Logikformats FOL-PRE. Die Datei dient als Referenz dafür, wie formale Ausdrücke aufgebaut sein müssen.

### Einfaches Beispiel

Teste diese beiden Beispiele. In beiden Fällen wird die LLM die Syntax erzeugen und die Prüfung starten.
```
Is it true that, if every swan is white, then every black animal is not a swan?
--> Ergebnis: FAILURE

Is it true that, if every swan is white and there are no black swans, then every black animal is not a swan?
--> Ergebnis: SUCCESS
```

### Beispiel SmartHome

Ein gut passendes SmartHome-Beispiel ist ein Luftfeuchtigkeits-Szenario für einen Kellerraum.
Das ist praktisch, weil das Ergebnis direkt eine Geräteaktion auslösen kann.

#### Beispiel fuer Axiome und Ziel

```
Axiome:
forall x ((LuftfeuchtigkeitHoch(x) and FensterGeschlossen(x)) -> EntfeuchterAn(x))
forall x ((LuftfeuchtigkeitNiedrig(x) and FensterGeschlossen(x)) -> EntfeuchterAus(x))
LuftfeuchtigkeitHoch(keller)
FensterGeschlossen(keller)

Ziel:
EntfeuchterAn(keller)
```

Bedeutung:

- Wenn in einem Raum die Luftfeuchtigkeit hoch ist und das Fenster geschlossen ist, soll der Entfeuchter laufen.
- Wenn in einem Raum die Luftfeuchtigkeit niedrig ist und das Fenster geschlossen ist, soll der Entfeuchter nicht laufen.
- Im Keller ist die Luftfeuchtigkeit hoch.
- Im Keller ist das Fenster geschlossen.
- Also soll aus den Axiomen folgen: `EntfeuchterAn(keller)`.

Das ist fuer externe Hardware gut geeignet, weil ein Device bei `SUCCESS` zum Beispiel den Entfeuchter einschalten oder ein Relay ansteuern kann.

#### API fuer Postman und externe Devices

Neben dem Chat-Endpunkt `/agent` gibt es jetzt auch einen strukturierten API-Endpunkt `/api/prove`.
Er ist für Maschinenaufrufe besser geeignet, weil er kompaktes JSON mit `status` und optional `device_action` zurückgibt.

##### Beispiel-Request in Postman

`POST http://localhost:5000/api/prove`

```json
{
  "device_id": "dehumidifier-keller",
  "axioms": [
    "forall x ((LuftfeuchtigkeitHoch(x) and FensterGeschlossen(x)) -> EntfeuchterAn(x))",
    "forall x ((LuftfeuchtigkeitNiedrig(x) and FensterGeschlossen(x)) -> EntfeuchterAus(x))",
    "LuftfeuchtigkeitHoch(keller)",
    "FensterGeschlossen(keller)"
  ],
  "goal": "EntfeuchterAn(keller)",
  "actions": {
    "on_success": "TURN_ON_DEHUMIDIFIER",
    "on_failure": "DO_NOTHING"
  }
}
```

##### Beispiel-Antwort bei erfolgreichem Beweis

```json
{
  "device_action": "TURN_ON_DEHUMIDIFIER",
  "device_id": "dehumidifier-keller",
  "status": "SUCCESS"
}
```

##### Beispiel-Antwort bei nicht beweisbarem Ziel

```json
{
  "device_action": "DO_NOTHING",
  "device_id": "dehumidifier-keller",
  "status": "FAILURE"
}
```

Ein externes Device oder ein nachgeschalteter Dienst kann dadurch sehr einfach reagieren:

- `status = SUCCESS`: Aktion ausführen
- `status = FAILURE`: keine Aktion oder Sicherheitsfallback ausführen

### Analyse von Verträgen

Im Ordner befinden sich die Dateien Mastervorlage.md und Beispielanalyse.md, welche für die Analyse von Verträgen verwendet werden können. Hierzu gibt es keine weiteren Informationen an dieser Stelle.