#!/usr/bin/env bash

mvn exec:java -Dexec.mainClass="net.openhft.collections.tutorial.statemachine.StateMachineTutorial" -Dexec.arguments=$@
