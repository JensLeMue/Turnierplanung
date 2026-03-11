package com.fencingplanner.model;

public enum AgeCategory {

VET,
SEN,
U23,
U20,
U17,
U15,
U14;

public boolean canStartIn(AgeCategory other){

if(this==other) return true;

switch(this){

case VET:
return other==SEN;

case U23:
return other==SEN;

case U20:
return other==U23 || other==SEN;

case U17:
return other==U20 || other==U23 || other==SEN;

case U15:
return other==U17;

case U14:
return other==U15;

default:
return false;

}

}

}