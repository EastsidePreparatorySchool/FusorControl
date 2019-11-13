$fn = 100;
difference(){
    union(){
        difference () {
            cube([32,30,7]);
            translate([6,0,1])cube([16,28,7]);
            translate([2,2,4])cube([24,26,4]);
            translate([28,25,7]) sphere(d=2, center=true);
        }

        //translate([-10,12.5,0])cube([15,15,2]);
    }
    //color("red") translate([27,10,7]) cylinder(10,d=7, center=true);
    color("blue") translate([27,10,0]) cylinder(35,d=4, center=true);
}

translate ([-1,0,9.5])
rotate([0,180,0])
difference(){
    difference () {
        translate([0,0,5.5])cube([32,30,4]);
        translate([10,0,5.5])cube([8,28,2]);
    }
    
    color("red") translate([27,10,12.5]) cylinder(10,d=10, center=true);
    color("blue") translate([27,10,0]) cylinder(35,d=3, center=true);
}
    
translate ([-1,0,9.5])
rotate([0,180,0])
translate([28,25,5.5]) 
color("red")
sphere(d=2, center=true);
