import { Component, HostListener, OnInit } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  breakpoint!: number;

  ngOnInit() {
    this.breakpoint = window.innerWidth <= 1600 ? window.innerWidth <= 1100  ? 1 : 2 : 3;
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    console.log(event.target.innerWidth)
    this.breakpoint = window.innerWidth <= 1600 ? window.innerWidth <= 1100  ? 1 : 2 : 3;
  }
}

