/**
 *  Garage After Dark
 *
 *  Author: Scottin Pollock
 *
 *  Date: 2014-06-15
 */
definition(
    name: "Garage After Dark",
    namespace: "smartthings",
    author: "Scotin Pollock",
    description: "Sends notification when garage door is open during sunset times.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
    section("Warn if garage door is open...") {
		input "multisensor", "capability.threeAxis", title: "Which?"
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", title: "Zip Code", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "message", "text", title: "Message to send...", required: false
	}
}

def installed() {
	initialize()
}

def updated() {
	unschedule()
	initialize()
}

def initialize() {
	scheduleAstroCheck()
	astroCheck()
}

def scheduleAstroCheck() {
	def min = Math.round(Math.floor(Math.random() * 60))
	def exp = "0 $min * * * ?"
    log.debug "$exp"
	schedule(exp, astroCheck) // check every hour since location can change without event?
    state.hasRandomSchedule = true
}

def astroCheck() {
	if (!state.hasRandomSchedule && state.riseTime) {
    	log.info "Rescheduling random astro check"
        unschedule("astroCheck")
    	scheduleAstroCheck()
    }
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
	if (state.riseTime != riseTime.time || state.setTime != setTime.time) {
		state.riseTime = riseTime.time
		state.setTime = setTime.time

		unschedule("sunriseHandler")
		unschedule("sunsetHandler")

		if (riseTime.after(now)) {
			log.info "scheduling sunrise handler for $riseTime"
			runOnce(riseTime, sunriseHandler)
		}

		if (setTime.after(now)) {
			log.info "scheduling sunset handler for $setTime"
			runOnce(setTime, sunsetHandler)
		}
	}
}

def sunriseHandler() {
	log.info "Executing sunrise handler"
	//Do Nothing
	unschedule("sunriseHandler") // Temporary work-around for scheduling bug
}

def sunsetHandler() {
	log.info "Executing sunset handler"
    def Gdoor = checkGarage()
       if (Gdoor == "open") {
       	send(message)
        }
	unschedule("sunsetHandler") // Temporary work-around for scheduling bug
}


private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( message )
	}

	log.debug message
}

def checkGarage(evt) {
def latestValue = multisensor.latestValue("status")
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}