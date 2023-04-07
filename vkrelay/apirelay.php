<?php
	//error_reporting(0);

	header("Content-Type: text/json");
	$request = file_get_contents("php://input");
	
	$curl = curl_init($request);
	curl_setopt(CURLOPT_RETURNTRANSFER, false);
	curl_exec($curl);