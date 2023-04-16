console.log("this is scipt file");

const toggleSidebar = () => {
	if ($(".sidebar").is(":visible")){
		//true

		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
		
	}else{
		//false
		//show karna hai
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
};