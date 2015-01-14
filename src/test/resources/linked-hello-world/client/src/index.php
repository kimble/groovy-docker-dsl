<?php
$ip = $_ENV["CALCULATOR_PORT_80_TCP_ADDR"];
echo "2 + 3 = " . file_get_contents("http://$ip/?a=2&b=3");