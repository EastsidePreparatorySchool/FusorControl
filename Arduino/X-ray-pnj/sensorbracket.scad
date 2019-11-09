$fn = 100;
difference(){
    union(){
        difference () {
            cube([30,30,7]);
            translate([7,0,1])cube([16,28,3]);
            translate([3,0,4])cube([24,28,4]);
        }

        //translate([-10,12.5,0])cube([15,15,2]);
    }
    color("red") translate([25,10,7]) cylinder(10,d=7, center=true);
    color("blue") translate([25,10,0]) cylinder(35,d=3, center=true);
}

difference(){
       //rotate([0,180,0])
        translate ([40,0,-5.5])difference () {
            translate([0,0,5.5])cube([30,30,4]);
            translate([11,0,5.5])cube([8,28,2]);
        }
    translate ([40,0,-5.5]){
        color("red") translate([25,10,7]) cylinder(10,d=7, center=true);
        color("blue") translate([25,10,0]) cylinder(35,d=3, center=true);
    }
}
            