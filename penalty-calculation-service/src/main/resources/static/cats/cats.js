const calculationCat = document.createElement('dialog');
calculationCat.id = 'calculation-loader';
calculationCat.className = 'cat-loader';
calculationCat.innerHTML = '<img src="/cats/cat-calculation.gif" alt="Кот на золотой монетке моргает и машет лапкой" width="288" height="384"><p id="cat-loading-text">Считаем…</p>';
calculationCat.addEventListener('cancel', event => event.preventDefault());
document.body.append(calculationCat);

function showCalculationCat() {
  calculationCat.showModal();
  return new Promise(resolve => setTimeout(resolve, 2000));
}

function hideCalculationCat() {
  calculationCat.close();
}

const catTail = document.createElement('button');
catTail.type = 'button';
catTail.className = 'cat-tail-button';
catTail.title = 'Показать кота';
catTail.innerHTML = '<span class="cat-tail"></span>';

const catSlide = document.createElement('aside');
catSlide.id = 'cat-slide';
catSlide.className = 'cat-slide';
catSlide.innerHTML = '<button class="cat-picture" type="button"><img src="/cats/cat-peek.jpg" alt="Кот на компьютерной мышке" width="280" height="365"></button><button class="cat-close" type="button">×</button>';
document.body.append(catTail, catSlide);

function toggleCat() {
  const open = !catSlide.classList.contains('is-open');
  catSlide.classList.toggle('is-open', open);
  catTail.classList.toggle('cat-is-open', open);
}

catTail.addEventListener('click', toggleCat);
catSlide.querySelector('.cat-picture').addEventListener('click', toggleCat);
catSlide.querySelector('.cat-close').addEventListener('click', toggleCat);
