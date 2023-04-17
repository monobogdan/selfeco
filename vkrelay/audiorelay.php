<?php
	/*
	*	AudioRelay for MiniVK. (C) 2023 monobogdan.
	*/
	
	$token = "vk1.a.Ywbi4d5sLVa5dyg2AqvdZhkmt0RfN48gUnxBG7mA5I3Oc0-qII9_xWQL73pbwR9shjNR-AFE8TM2Ch6CKmv3PJfpqEyqizn6qT4zcZnkPzui4SG2ZD6hS9PHyGpO9pizG965FHelURkF3C4CzfdIDb7bvjX0qFPo2L8TjwQD6jkGOaGWursjK4lS7kydHXoZnhqSRysaIZZgegyFdc7tFQ";
	
	function vkRequest($request)
	{
		global $token;
		
		$curl = curl_init("https://api.vk.com/method/" . $request . "access_token=$token&v=5.131");
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
		return curl_exec($curl);
	}
	
	function audioGet()
	{
		$uid = $_GET["uid"];
		
		return vkRequest("audio.get?owner_id=$uid&count=100&");
	}
	
	function audioSearch()
	{
		$query = $_GET["query"];
		
		return vkRequest("audio.search?q=$query&count=100&");
	}
	
	function audioGetDetails()
	{
		$id = $_GET["id"];
		
		return vkRequest("audio.getById?audios=$id&");
	}
	
	function audioAdd()
	{
		$id = $_GET["id"];
		$owner = substr($id, strpos($id, '_'));
		$tId = substr($id, strpos($id, '_'), strlen($id) - strpos($id, '_'));
		
		return vkRequest("audio.add?audio_id=$tId&owner_id=$owner");
	}
	
	function audioStream()
	{
		$url = $_GET["url"];
		
		$curl = curl_init(urldecode($url));
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
		return curl_exec($curl);
	}
	
	$actions = array();
	$actions["get"] = 'audioGet';
	$actions["search"] = 'audioSearch';
	$actions["getDetails"] = 'audioGetDetails';
	$actions["stream"] = 'audioStream';
	
	if(isset($_GET["act"]))
	{
		$act = $_GET["act"];
		
		if(isset($actions[$act]))
			exit($actions[$act]());
	}
	
	exit("INTERNAL_ERROR");