on run argv
	tell application "System Events"
		if exists process "Live" then
			set frontmost of process "Live" to true
			keystroke "q" using command down
			tell application "System Events" to keystroke (ASCII character 28) --left arrow
			tell application "System Events" to keystroke (ASCII character 28) --left arrow
			keystroke return
			delay 5.0
		end if
	end tell
end run
